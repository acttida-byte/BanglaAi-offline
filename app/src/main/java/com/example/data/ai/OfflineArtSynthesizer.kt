package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.domain.ArtAspect
import com.example.domain.ArtStyle
import com.example.domain.GeneratedImageResult
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 100% offline procedural Canvas artwork synthesizer driven by Bengali/English prompt semantics
 * and artistic style selections. Renders crisp high-resolution bitmaps locally without network.
 */
class OfflineArtSynthesizer(private val context: Context) {

    private enum class SceneTheme {
        SUNSET_MOUNTAIN, NIGHT_GALAXY, RIVER_NATURE, FIRE_PASSION, FLORAL_GARDEN, CYBER_ABSTRACT
    }

    fun generateArtwork(
        prompt: String,
        style: ArtStyle,
        aspect: ArtAspect
    ): GeneratedImageResult {
        val cleanPrompt = prompt.trim().ifEmpty { "রঙিন বাংলা প্রকৃতি" }
        val theme = detectTheme(cleanPrompt, style)
        val seed = cleanPrompt.hashCode() xor style.ordinal * 31
        val rng = Random(seed)

        val width = aspect.width
        val height = aspect.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackdrop(canvas, width, height, theme, style)
        drawCelestialOrGlow(canvas, width, height, theme, style, rng)

        when (theme) {
            SceneTheme.SUNSET_MOUNTAIN -> {
                drawMountains(canvas, width, height, rng, warm = true)
                drawWaterReflection(canvas, width, height, Color.parseColor("#38BDF8"), Color.parseColor("#FB923C"))
            }
            SceneTheme.NIGHT_GALAXY -> {
                drawStars(canvas, width, height, rng, count = 110)
                drawMountains(canvas, width, height, rng, warm = false)
            }
            SceneTheme.RIVER_NATURE -> {
                drawHills(canvas, width, height, rng)
                drawWaterReflection(canvas, width, height, Color.parseColor("#0EA5E9"), Color.parseColor("#2DD4BF"))
            }
            SceneTheme.FIRE_PASSION -> {
                drawFlameRibbons(canvas, width, height, rng)
            }
            SceneTheme.FLORAL_GARDEN -> {
                drawBotanicalMandala(canvas, width, height, rng)
            }
            SceneTheme.CYBER_ABSTRACT -> {
                drawCyberGridAndOrbs(canvas, width, height, rng)
            }
        }

        if (style == ArtStyle.WATERCOLOR) {
            drawSoftWatercolorBlobs(canvas, width, height, rng)
        }

        drawPromptBadge(canvas, width, height, cleanPrompt, style)

        val dir = File(context.filesDir, "images").apply { mkdirs() }
        val file = File(dir, "art_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        bitmap.recycle()

        return GeneratedImageResult(
            filePath = file.absolutePath,
            title = cleanPrompt.take(36),
            prompt = cleanPrompt,
            isOfflineArtwork = true
        )
    }

    private fun detectTheme(prompt: String, style: ArtStyle): SceneTheme {
        val p = prompt.lowercase()
        return when {
            p.containsAny("আগুন", "রাগ", "লাল", "শিখা", "fire", "flame", "angry", "red") -> SceneTheme.FIRE_PASSION
            p.containsAny("রাত", "চাঁদ", "তারা", "মহাকাশ", "গ্যালাক্সি", "night", "moon", "star", "space") -> SceneTheme.NIGHT_GALAXY
            p.containsAny("ফুল", "বাগান", "গোলাপ", "পদ্ম", "flower", "garden", "lotus") -> SceneTheme.FLORAL_GARDEN
            p.containsAny("নদী", "সমুদ্র", "পানি", "বৃষ্টি", "সবুজ", "river", "ocean", "water", "rain") -> SceneTheme.RIVER_NATURE
            p.containsAny("পাহাড়", "পাহাড়", "সূর্য", "ভোর", "বিকেল", "mountain", "sunset", "sun") -> SceneTheme.SUNSET_MOUNTAIN
            style == ArtStyle.NEON_CYBER -> SceneTheme.CYBER_ABSTRACT
            style == ArtStyle.SUNSET_DREAM -> SceneTheme.SUNSET_MOUNTAIN
            else -> SceneTheme.CYBER_ABSTRACT
        }
    }

    private fun drawBackdrop(
        canvas: Canvas,
        w: Int,
        h: Int,
        theme: SceneTheme,
        style: ArtStyle
    ) {
        val colors = when (theme) {
            SceneTheme.SUNSET_MOUNTAIN -> intArrayOf(
                Color.parseColor("#1E1B4B"),
                Color.parseColor("#BE185D"),
                Color.parseColor("#FB923C")
            )
            SceneTheme.NIGHT_GALAXY -> intArrayOf(
                Color.parseColor("#020617"),
                Color.parseColor("#0F172A"),
                Color.parseColor("#1E3A8A")
            )
            SceneTheme.RIVER_NATURE -> intArrayOf(
                Color.parseColor("#064E3B"),
                Color.parseColor("#0D9488"),
                Color.parseColor("#38BDF8")
            )
            SceneTheme.FIRE_PASSION -> intArrayOf(
                Color.parseColor("#1C0709"),
                Color.parseColor("#991B1B"),
                Color.parseColor("#F97316")
            )
            SceneTheme.FLORAL_GARDEN -> intArrayOf(
                Color.parseColor("#3B0764"),
                Color.parseColor("#9D174D"),
                Color.parseColor("#FDE68A")
            )
            SceneTheme.CYBER_ABSTRACT -> if (style == ArtStyle.MINIMAL_VECTOR) {
                intArrayOf(
                    Color.parseColor("#0F172A"),
                    Color.parseColor("#134E4A"),
                    Color.parseColor("#1E293B")
                )
            } else {
                intArrayOf(
                    Color.parseColor("#090D16"),
                    Color.parseColor("#312E81"),
                    Color.parseColor("#0D9488")
                )
            }
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat() * 0.25f, h.toFloat(),
                colors, null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
    }

    private fun drawCelestialOrGlow(
        canvas: Canvas,
        w: Int,
        h: Int,
        theme: SceneTheme,
        style: ArtStyle,
        rng: Random
    ) {
        val cx = w * (0.35f + rng.nextFloat() * 0.3f)
        val cy = h * 0.32f
        val radius = minOf(w, h) * 0.22f
        val (coreColor, haloColor) = when (theme) {
            SceneTheme.NIGHT_GALAXY -> Color.parseColor("#F8FAFC") to Color.parseColor("#60A5FA")
            SceneTheme.FIRE_PASSION -> Color.parseColor("#FEF08A") to Color.parseColor("#EF4444")
            SceneTheme.RIVER_NATURE -> Color.parseColor("#FEF9C3") to Color.parseColor("#2DD4BF")
            else -> Color.parseColor("#FDE047") to Color.parseColor("#FB7185")
        }

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, radius * 2.1f,
                intArrayOf(haloColor, Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
            alpha = 180
        }
        canvas.drawCircle(cx, cy, radius * 2.1f, glowPaint)

        val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = coreColor
            if (style == ArtStyle.WATERCOLOR) {
                maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
            }
        }
        canvas.drawCircle(cx, cy, radius * 0.55f, orbPaint)
    }

    private fun drawMountains(canvas: Canvas, w: Int, h: Int, rng: Random, warm: Boolean) {
        val layerColors = if (warm) {
            listOf(Color.parseColor("#581C87"), Color.parseColor("#311042"), Color.parseColor("#111827"))
        } else {
            listOf(Color.parseColor("#1E293B"), Color.parseColor("#0F172A"), Color.parseColor("#020617"))
        }
        layerColors.forEachIndexed { idx, col ->
            val path = Path()
            val baseY = h * (0.56f + idx * 0.08f)
            path.moveTo(0f, h.toFloat())
            path.lineTo(0f, baseY)
            val steps = 7
            for (i in 1..steps) {
                val x = (w.toFloat() / steps) * i
                val peakOffset = (rng.nextFloat() * 0.16f - 0.08f) * h
                path.lineTo(x, (baseY + peakOffset).coerceIn(h * 0.35f, h * 0.85f))
            }
            path.lineTo(w.toFloat(), h.toFloat())
            path.close()

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = col
                alpha = 235
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawHills(canvas: Canvas, w: Int, h: Int, rng: Random) {
        val cols = listOf(Color.parseColor("#065F46"), Color.parseColor("#047857"))
        cols.forEachIndexed { idx, col ->
            val path = Path()
            val y = h * (0.52f + idx * 0.07f)
            path.moveTo(0f, h.toFloat())
            path.lineTo(0f, y)
            path.cubicTo(
                w * 0.25f, y - h * (0.06f + rng.nextFloat() * 0.05f),
                w * 0.65f, y + h * 0.05f,
                w.toFloat(), y - h * 0.03f
            )
            path.lineTo(w.toFloat(), h.toFloat())
            path.close()
            canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col })
        }
    }

    private fun drawWaterReflection(canvas: Canvas, w: Int, h: Int, topColor: Int, shimmerColor: Int) {
        val waterTop = h * 0.70f
        val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, waterTop, 0f, h.toFloat(),
                topColor, Color.parseColor("#090D16"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, waterTop, w.toFloat(), h.toFloat(), waterPaint)

        val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shimmerColor
            strokeWidth = 3f
            alpha = 120
        }
        for (i in 0 until 14) {
            val ry = waterTop + (h - waterTop) * (i / 14f)
            val span = w * (0.18f + (i % 3) * 0.09f)
            val rx = (w - span) / 2f + ((i % 2) * 30f - 15f)
            canvas.drawLine(rx, ry, rx + span, ry, ripplePaint)
        }
    }

    private fun drawStars(canvas: Canvas, w: Int, h: Int, rng: Random, count: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        repeat(count) {
            val sx = rng.nextFloat() * w
            val sy = rng.nextFloat() * h * 0.68f
            val r = 1.2f + rng.nextFloat() * 3.5f
            paint.alpha = 110 + rng.nextInt(145)
            canvas.drawCircle(sx, sy, r, paint)
        }
    }

    private fun drawFlameRibbons(canvas: Canvas, w: Int, h: Int, rng: Random) {
        val colors = listOf(Color.parseColor("#EF4444"), Color.parseColor("#F59E0B"), Color.parseColor("#FDE047"))
        colors.forEachIndexed { i, col ->
            val path = Path()
            val startX = w * (0.25f + i * 0.22f)
            path.moveTo(startX, h.toFloat())
            path.cubicTo(
                startX - w * 0.18f, h * 0.65f,
                startX + w * 0.2f, h * 0.4f,
                w * (0.4f + rng.nextFloat() * 0.2f), h * 0.14f
            )
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 28f - i * 6f
                color = col
                alpha = 175
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawPath(path, p)
        }
    }

    private fun drawBotanicalMandala(canvas: Canvas, w: Int, h: Int, rng: Random) {
        val cx = w / 2f
        val cy = h * 0.48f
        val petals = 12
        val maxR = minOf(w, h) * 0.30f
        val petalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FDA4AF")
            alpha = 190
        }
        for (i in 0 until petals) {
            val angle = (2.0 * PI * i) / petals
            val px = cx + (cos(angle) * maxR * 0.52).toFloat()
            val py = cy + (sin(angle) * maxR * 0.52).toFloat()
            petalPaint.color = if (i % 2 == 0) Color.parseColor("#FB7185") else Color.parseColor("#FDE047")
            canvas.drawCircle(px, py, maxR * 0.28f + rng.nextFloat() * 8f, petalPaint)
        }
        canvas.drawCircle(cx, cy, maxR * 0.24f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FEF08A")
        })
    }

    private fun drawCyberGridAndOrbs(canvas: Canvas, w: Int, h: Int, rng: Random) {
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2DD4BF")
            strokeWidth = 2f
            alpha = 65
        }
        val step = 64f
        var x = 0f
        while (x < w) {
            canvas.drawLine(x, 0f, x, h.toFloat(), gridPaint)
            x += step
        }
        var y = 0f
        while (y < h) {
            canvas.drawLine(0f, y, w.toFloat(), y, gridPaint)
            y += step
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        repeat(8) {
            accentPaint.color = if (it % 2 == 0) Color.parseColor("#14B8A6") else Color.parseColor("#818CF8")
            accentPaint.alpha = 140
            val ox = rng.nextFloat() * w
            val oy = rng.nextFloat() * h * 0.78f
            val r = 24f + rng.nextFloat() * 68f
            canvas.drawCircle(ox, oy, r, accentPaint)
        }
    }

    private fun drawSoftWatercolorBlobs(canvas: Canvas, w: Int, h: Int, rng: Random) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            maskFilter = BlurMaskFilter(40f, BlurMaskFilter.Blur.NORMAL)
        }
        repeat(6) {
            paint.color = if (it % 2 == 0) Color.parseColor("#2DD4BF") else Color.parseColor("#FB7185")
            paint.alpha = 45
            canvas.drawCircle(
                rng.nextFloat() * w,
                rng.nextFloat() * h,
                minOf(w, h) * 0.18f,
                paint
            )
        }
    }

    private fun drawPromptBadge(
        canvas: Canvas,
        w: Int,
        h: Int,
        prompt: String,
        style: ArtStyle
    ) {
        val barHeight = h * 0.14f
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(175, 9, 13, 22)
        }
        canvas.drawRect(0f, h - barHeight, w.toFloat(), h.toFloat(), barPaint)

        val typeface = ResourcesCompat.getFont(context, R.font.hind_siliguri) ?: Typeface.DEFAULT_BOLD
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (w * 0.032f).coerceIn(24f, 38f)
            this.typeface = typeface
        }
        val label = "$prompt  •  ${style.labelBn}"
        val textWidth = (w - 64).coerceAtLeast(100)
        val layout = StaticLayout.Builder.obtain(label, 0, label.length, textPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(2)
            .build()
        canvas.save()
        canvas.translate(32f, h - barHeight + (barHeight - layout.height) / 2f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun String.containsAny(vararg words: String): Boolean = words.any { this.contains(it) }
}
