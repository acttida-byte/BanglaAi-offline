package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ai.BengaliTextProcessor
import com.example.domain.ArtAspect
import com.example.domain.ArtStyle
import com.example.domain.MediaType
import com.example.domain.SavedCreation
import com.example.ui.BondhuUiState
import com.example.ui.MediaSubTab
import com.example.ui.components.FirebaseNoticeBanner
import java.io.File

@Composable
fun MediaStudioScreen(
    uiState: BondhuUiState,
    onSelectSubTab: (MediaSubTab) -> Unit,
    onImagePromptChange: (String) -> Unit,
    onSelectArtStyle: (ArtStyle) -> Unit,
    onSelectArtAspect: (ArtAspect) -> Unit,
    onGenerateImage: () -> Unit,
    onUseImageInVideo: (String, String) -> Unit,
    onAddPickedPhotos: (List<String>) -> Unit,
    onRemoveVideoSlide: (String) -> Unit,
    onVideoTitleChange: (String) -> Unit,
    onVideoCaptionChange: (String) -> Unit,
    onVideoDurationChange: (Int) -> Unit,
    onGenerateVideo: () -> Unit,
    onOpenVideoPlayer: (String?) -> Unit,
    onDeleteCreation: (SavedCreation) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onAddPickedPhotos(uris.map { it.toString() })
        }
    }

    if (uiState.activeVideoPlaybackPath != null) {
        BackHandler { onOpenVideoPlayer(null) }
        VideoPlayerDialog(
            filePath = uiState.activeVideoPlaybackPath,
            onDismiss = { onOpenVideoPlayer(null) },
            onShare = { shareMediaFile(context, uiState.activeVideoPlaybackPath, "video/mp4") }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Studio Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(22.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_studio_banner),
                    contentDescription = "মিডিয়া স্টুডিও ব্যানার",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xCC090D16),
                                    Color(0x88090D16),
                                    Color(0x33090D16)
                                )
                            )
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Text(
                            text = "অফলাইন ছবি ও ভিডিও স্টুডিও",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ইন্টারনেট ছাড়াই আর্ট তৈরি করুন ও আসল MP4 ভিডিও বানান",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            // Segmented Switcher
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf(
                    MediaSubTab.IMAGE to stringResource(R.string.media_tab_image),
                    MediaSubTab.VIDEO to stringResource(R.string.media_tab_video),
                    MediaSubTab.GALLERY to "${stringResource(R.string.media_tab_gallery)} (${BengaliTextProcessor.toBengaliDigits(uiState.savedCreations.size)})"
                )
                tabs.forEachIndexed { index, (tab, label) ->
                    SegmentedButton(
                        selected = uiState.mediaSubTab == tab,
                        onClick = { onSelectSubTab(tab) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        modifier = Modifier.testTag("media_subtab_${tab.name.lowercase()}")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                }
            }

            when (uiState.mediaSubTab) {
                MediaSubTab.IMAGE -> ImageCreationSection(
                    uiState = uiState,
                    onPromptChange = onImagePromptChange,
                    onSelectStyle = onSelectArtStyle,
                    onSelectAspect = onSelectArtAspect,
                    onGenerate = onGenerateImage,
                    onUseInVideo = onUseImageInVideo,
                    onShareImage = { path -> shareMediaFile(context, path, "image/png") }
                )

                MediaSubTab.VIDEO -> VideoCreationSection(
                    uiState = uiState,
                    onPickPhotos = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onAddFromStudioGallery = { creation ->
                        onUseImageInVideo(creation.filePath, creation.title)
                    },
                    onRemoveSlide = onRemoveVideoSlide,
                    onTitleChange = onVideoTitleChange,
                    onCaptionChange = onVideoCaptionChange,
                    onDurationChange = onVideoDurationChange,
                    onGenerateVideo = onGenerateVideo
                )

                MediaSubTab.GALLERY -> SavedGallerySection(
                    creations = uiState.savedCreations,
                    onPlayVideo = { onOpenVideoPlayer(it.filePath) },
                    onUseInVideo = { onUseImageInVideo(it.filePath, it.title) },
                    onShare = {
                        val mime = if (it.mediaType == MediaType.VIDEO) "video/mp4" else "image/png"
                        shareMediaFile(context, it.filePath, mime)
                    },
                    onDelete = onDeleteCreation
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImageCreationSection(
    uiState: BondhuUiState,
    onPromptChange: (String) -> Unit,
    onSelectStyle: (ArtStyle) -> Unit,
    onSelectAspect: (ArtAspect) -> Unit,
    onGenerate: () -> Unit,
    onUseInVideo: (String, String) -> Unit,
    onShareImage: (String) -> Unit
) {
    val samplePrompts = listOf(
        "পাহাড়ে সুন্দর সূর্যাস্ত",
        "রাতের আকাশে চাঁদ ও তারা",
        "নদীর ধারে সবুজ বাগান ও ফুল",
        "আগুনের শিখা ও রাগ",
        "নিয়ন সাইবার ফিউচার শহর"
    )

    FirebaseNoticeBanner(
        text = "অফলাইন আর্ট স্টুডিও সক্রিয় • ক্লাউড জেনারেটিভ জেমিনি ছবির জন্য ফায়ারবেস প্রয়োজন"
    )

    OutlinedTextField(
        value = uiState.imagePrompt,
        onValueChange = onPromptChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("image_prompt_input"),
        label = { Text("ছবির বর্ণনা (বাংলায়)") },
        placeholder = { Text(stringResource(R.string.image_prompt_hint)) },
        shape = RoundedCornerShape(16.dp),
        maxLines = 3
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(samplePrompts) { sample ->
            AssistChip(
                onClick = { onPromptChange(sample) },
                label = { Text(sample, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }

    Text(
        text = "শিল্পের ধরন (Style):",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ArtStyle.entries.forEach { style ->
            FilterChip(
                selected = uiState.selectedArtStyle == style,
                onClick = { onSelectStyle(style) },
                label = { Text(style.labelBn) },
                modifier = Modifier.minimumInteractiveComponentSize()
            )
        }
    }

    Text(
        text = "মাপ (Aspect Ratio):",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ArtAspect.entries.forEach { aspect ->
            FilterChip(
                selected = uiState.selectedArtAspect == aspect,
                onClick = { onSelectAspect(aspect) },
                label = { Text(aspect.labelBn) },
                modifier = Modifier.minimumInteractiveComponentSize()
            )
        }
    }

    if (!uiState.imageError.isNullOrBlank()) {
        Text(
            text = uiState.imageError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }

    Button(
        onClick = onGenerate,
        enabled = !uiState.isGeneratingImage,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("generate_image_button"),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (uiState.isGeneratingImage) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text("অফলাইনে ছবি আঁকা হচ্ছে...")
        } else {
            Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.image_generate_btn))
        }
    }

    val latest = uiState.latestGeneratedImage
    if (latest != null) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = File(latest.filePath),
                    contentDescription = latest.prompt,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onShareImage(latest.filePath) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("শেয়ার")
                    }
                    Button(
                        onClick = { onUseInVideo(latest.filePath, latest.title) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ভিডিও বানান")
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoCreationSection(
    uiState: BondhuUiState,
    onPickPhotos: () -> Unit,
    onAddFromStudioGallery: (SavedCreation) -> Unit,
    onRemoveSlide: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onCaptionChange: (String) -> Unit,
    onDurationChange: (Int) -> Unit,
    onGenerateVideo: () -> Unit
) {
    val studioImages = uiState.savedCreations.filter { it.mediaType == MediaType.IMAGE }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "১. ভিডিওর জন্য ছবি যোগ করুন (সর্বোচ্চ ৫টি)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = onPickPhotos,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("pick_photos_for_video_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Filled.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.video_pick_photos_btn))
            }

            if (studioImages.isNotEmpty()) {
                Text(
                    text = "অথবা স্টুডিওর তৈরি ছবি থেকে বেছে নিন:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(studioImages, key = { it.id }) { img ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAddFromStudioGallery(img) }
                        ) {
                            AsyncImage(
                                model = File(img.filePath),
                                contentDescription = img.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            if (uiState.selectedVideoSlides.isNotEmpty()) {
                Text(
                    text = "নির্বাচিত স্লাইড (${BengaliTextProcessor.toBengaliDigits(uiState.selectedVideoSlides.size)}টি):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.selectedVideoSlides, key = { it.uriOrPath }) { slide ->
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .clip(RoundedCornerShape(14.dp))
                        ) {
                            val model: Any = if (slide.isFilePath) File(slide.uriOrPath) else Uri.parse(slide.uriOrPath)
                            AsyncImage(
                                model = model,
                                contentDescription = slide.label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { onRemoveSlide(slide.uriOrPath) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "সরিয়ে দিন",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "কোনো ছবি না দিলে ডিফল্ট অ্যানিমেটেড থিম ব্যাকগ্রাউন্ড দিয়ে ভিডিও তৈরি হবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    OutlinedTextField(
        value = uiState.videoTitle,
        onValueChange = onTitleChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_title_input"),
        label = { Text("ভিডিওর শিরোনাম") },
        placeholder = { Text(stringResource(R.string.video_title_hint)) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp)
    )

    OutlinedTextField(
        value = uiState.videoCaption,
        onValueChange = onCaptionChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_caption_input"),
        label = { Text("ভিডিওর ভেতরের বাংলা বার্তা") },
        placeholder = { Text(stringResource(R.string.video_caption_hint)) },
        maxLines = 2,
        shape = RoundedCornerShape(16.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "প্রতি স্লাইডের সময়:",
            style = MaterialTheme.typography.labelLarge
        )
        listOf(2, 3, 4).forEach { sec ->
            FilterChip(
                selected = uiState.videoSlideSeconds == sec,
                onClick = { onDurationChange(sec) },
                label = { Text("${BengaliTextProcessor.toBengaliDigits(sec)} সে.") },
                modifier = Modifier.minimumInteractiveComponentSize()
            )
        }
    }

    if (uiState.isGeneratingVideo) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LinearProgressIndicator(
                progress = { uiState.videoProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
            )
            Text(
                text = "অফলাইন H.264 MP4 ভিডিও তৈরি হচ্ছে... ${BengaliTextProcessor.toBengaliDigits((uiState.videoProgress * 100).toInt())}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (!uiState.videoError.isNullOrBlank()) {
        Text(
            text = uiState.videoError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }

    Button(
        onClick = onGenerateVideo,
        enabled = !uiState.isGeneratingVideo,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("generate_video_button"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(imageVector = Icons.Filled.Movie, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.video_generate_btn))
    }
}

@Composable
private fun SavedGallerySection(
    creations: List<SavedCreation>,
    onPlayVideo: (SavedCreation) -> Unit,
    onUseInVideo: (SavedCreation) -> Unit,
    onShare: (SavedCreation) -> Unit,
    onDelete: (SavedCreation) -> Unit
) {
    if (creations.isEmpty()) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.VideoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = "গ্যালারি এখন খালি",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "অফলাইনে নতুন ছবি বা MP4 ভিডিও তৈরি করলে এখানে নিরাপদে সেভ থাকবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        creations.forEach { item ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                if (item.mediaType == MediaType.VIDEO) onPlayVideo(item)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.mediaType == MediaType.IMAGE) {
                            AsyncImage(
                                model = File(item.filePath),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "ভিডিও চালান",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (item.mediaType == MediaType.VIDEO) {
                                "MP4 ভিডিও • ${BengaliTextProcessor.toBengaliDigits(item.durationSeconds)} সেকেন্ড"
                            } else {
                                "অফলাইন আর্ট ছবি"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row {
                        if (item.mediaType == MediaType.VIDEO) {
                            IconButton(
                                onClick = { onPlayVideo(item) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "চালান",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { onUseInVideo(item) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Movie,
                                    contentDescription = "ভিডিওতে নিন",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        IconButton(
                            onClick = { onShare(item) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "শেয়ার করুন"
                            )
                        }
                        IconButton(
                            onClick = { onDelete(item) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "মুছুন",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPlayerDialog(
    filePath: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "অফলাইন ভিডিও প্লেয়ার",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "বন্ধ করুন")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoPath(filePath)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                    isPlaying = true
                                }
                                videoViewRef = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val vv = videoViewRef
                            if (vv != null) {
                                if (vv.isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                } else {
                                    vv.start()
                                    isPlaying = true
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPlaying) "বিরতি" else "চালান")
                    }
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("শেয়ার করুন")
                    }
                }
            }
        }
    }
}

private fun shareMediaFile(context: Context, filePath: String, mimeType: String) {
    runCatching {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "শেয়ার করুন"))
    }
}
