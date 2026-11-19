package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

data class VideoSlideInput(
    val uriOrPath: String,
    val isFilePath: Boolean,
    val caption: String
)

data class VideoOutputResult(
    val filePath: String,
    val durationSeconds: Int,
    val title: String
)

/**
 * 100% offline on-device H.264 MP4 video generator using Android MediaCodec Surface input + MediaMuxer.
 * Supports Ken Burns zoom/pan transitions and Bengali title/caption overlays.
 */
class OfflineVideoSynthesizer(private val context: Context) {

    companion object {
        private const val WIDTH = 720
        private const val HEIGHT = 720
        private const val FPS = 24
        private const val BIT_RATE = 2_400_000
        private const val I_FRAME_INTERVAL = 1
        private const val TIMEOUT_US = 10_000L
    }

    suspend fun createMp4Video(
        title: String,
        slides: List<VideoSlideInput>,
        secondsPerSlide: Int,
        onProgress: (Float) -> Unit
    ): VideoOutputResult = withContext(Dispatchers.Default) {
        val loadedSlides = slides.mapNotNull { slide ->
            loadScaledBitmap(slide.uriOrPath, slide.isFilePath)?.let { bmp ->
                bmp to slide.caption
            }
        }
        val effectiveSlides = if (loadedSlides.isNotEmpty()) {
            loadedSlides
        } else {
            listOf(createDefaultArtSlide(title) to title)
        }

        val outDir = File(context.filesDir, "videos").apply { mkdirs() }
        val outputFile = File(outDir, "bondhu_video_${System.currentTimeMillis()}.mp4")

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var trackIndex = -1
        val bufferInfo = MediaCodec.BufferInfo()
        val typeface = ResourcesCompat.getFont(context, R.font.hind_siliguri) ?: Typeface.DEFAULT_BOLD

        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val framesPerSlide = (secondsPerSlide.coerceIn(2, 5)) * FPS
            val totalFrames = effectiveSlides.size * framesPerSlide
            val frameDurationUs = 1_000_000L / FPS
            var frameIndex = 0

            fun drain(endOfStream: Boolean) {
                if (endOfStream) {
                    encoder.signalEndOfInputStream()
                }
                while (true) {
                    val outIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    when {
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (!endOfStream) break
                        }
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (!muxerStarted) {
                                trackIndex = muxer!!.addTrack(encoder.outputFormat)
                                muxer.start()
                                muxerStarted = true
                            }
                        }
                        outIndex >= 0 -> {
                            val encodedData = encoder.getOutputBuffer(outIndex)
                            if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                    encodedData.position(bufferInfo.offset)
                                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                    bufferInfo.presentationTimeUs = frameIndex * frameDurationUs
                                    muxer?.writeSampleData(trackIndex, encodedData, bufferInfo)
                                }
                            }
                            encoder.releaseOutputBuffer(outIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break
                            }
                        }
                    }
                }
            }

            for ((slideIdx, pair) in effectiveSlides.withIndex()) {
                val (bitmap, caption) = pair
                for (f in 0 until framesPerSlide) {
                    val progressInSlide = f.toFloat() / framesPerSlide.toFloat()
                    val canvas = inputSurface.lockCanvas(null)
                    try {
                        renderVideoFrame(
                            canvas = canvas,
                            bitmap = bitmap,
                            title = title,
                            caption = caption.ifBlank { title },
                            slideProgress = progressInSlide,
                            slideNumber = slideIdx + 1,
                            totalSlides = effectiveSlides.size,
                            typeface = typeface
                        )
                    } finally {
                        inputSurface.unlockCanvasAndPost(canvas)
                    }
                    drain(endOfStream = false)
                    frameIndex++
                    if (frameIndex % 4 == 0 || frameIndex == totalFrames) {
                        onProgress((frameIndex.toFloat() / totalFrames.toFloat()).coerceIn(0f, 1f))
                    }
                }
            }
            drain(endOfStream = true)
            onProgress(1f)
        } finally {
            effectiveSlides.forEach { it.first.recycle() }
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
        }

        VideoOutputResult(
            filePath = outputFile.absolutePath,
            durationSeconds = effectiveSlides.size * secondsPerSlide.coerceIn(2, 5),
            title = title.ifBlank { "বন্ধু এআই ভিডিও" }
        )
    }

    private fun renderVideoFrame(
        canvas: Canvas,
        bitmap: Bitmap,
        title: String,
        caption: String,
        slideProgress: Float,
        slideNumber: Int,
        totalSlides: Int,
        typeface: Typeface
    ) {
        canvas.drawColor(Color.parseColor("#090D16"))

        // Smooth Ken Burns zoom (1.0x -> 1.12x)
        val zoom = 1.0f + slideProgress * 0.12f
        val bmpW = bitmap.width.toFloat()
        val bmpH = bitmap.height.toFloat()
        val scale = maxOf(WIDTH / bmpW, HEIGHT / bmpH) * zoom
        val drawW = bmpW * scale
        val drawH = bmpH * scale
        val dx = (WIDTH - drawW) / 2f
        val dy = (HEIGHT - drawH) / 2f
        val dst = RectF(dx, dy, dx + drawW, dy + drawH)
        canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), dst, Paint(Paint.FILTER_BITMAP_FLAG))

        // Subtle fade in/out overlay
        val fadeAlpha = when {
            slideProgress < 0.12f -> ((1f - slideProgress / 0.12f) * 180).toInt()
            slideProgress > 0.88f -> (((slideProgress - 0.88f) / 0.12f) * 180).toInt()
            else -> 0
        }.coerceIn(0, 220)
        if (fadeAlpha > 0) {
            canvas.drawColor(Color.argb(fadeAlpha, 9, 13, 22))
        }

        // Top & bottom gradient scrims for crisp Bengali legibility
        val bottomScrim = Paint().apply {
            shader = LinearGradient(
                0f, HEIGHT * 0.62f, 0f, HEIGHT.toFloat(),
                Color.TRANSPARENT, Color.argb(230, 9, 13, 22),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, HEIGHT * 0.62f, WIDTH.toFloat(), HEIGHT.toFloat(), bottomScrim)

        // Header chip
        if (title.isNotBlank()) {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(190, 15, 23, 42)
            }
            canvas.drawRoundRect(RectF(28f, 28f, WIDTH - 28f, 88f), 20f, 20f, badgePaint)
            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#5EEAD4")
                textSize = 26f
                this.typeface = typeface
            }
            val headerText = "$title  (${BengaliTextProcessor.toBengaliDigits(slideNumber)}/${BengaliTextProcessor.toBengaliDigits(totalSlides)})"
            canvas.drawText(headerText.take(36), 48f, 67f, titlePaint)
        }

        // Caption overlay
        if (caption.isNotBlank()) {
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 32f
                this.typeface = typeface
            }
            val layout = StaticLayout.Builder.obtain(caption, 0, caption.length, textPaint, WIDTH - 80)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(3)
                .build()
            canvas.save()
            canvas.translate(40f, HEIGHT - layout.height - 48f)
            layout.draw(canvas)
            canvas.restore()
        }
    }

    private fun loadScaledBitmap(uriOrPath: String, isFilePath: Boolean): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            if (isFilePath) {
                BitmapFactory.decodeFile(uriOrPath, opts)
            } else {
                context.contentResolver.openInputStream(Uri.parse(uriOrPath))?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                }
            }
            val maxDim = maxOf(opts.outWidth, opts.outHeight).coerceAtLeast(1)
            val sample = (maxDim / min(WIDTH, HEIGHT)).coerceAtLeast(1)
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            if (isFilePath) {
                BitmapFactory.decodeFile(uriOrPath, decodeOpts)
            } else {
                context.contentResolver.openInputStream(Uri.parse(uriOrPath))?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun createDefaultArtSlide(title: String): Bitmap {
        val bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
                intArrayOf(Color.parseColor("#0F172A"), Color.parseColor("#0D9488"), Color.parseColor("#4F46E5")),
                null,
                Shader.TileMode.CLAMP
            )
        }
        c.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), p)
        return bmp
    }
}
