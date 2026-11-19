package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.BengaliTextProcessor
import com.example.data.ai.OfflineAiEngineRepository
import com.example.data.ai.OfflineArtSynthesizer
import com.example.data.ai.OfflineVideoSynthesizer
import com.example.data.ai.VideoSlideInput
import com.example.data.local.BondhuDatabase
import com.example.data.local.VaultDataRepository
import com.example.domain.AiMood
import com.example.domain.AiRepository
import com.example.domain.ArtAspect
import com.example.domain.ArtStyle
import com.example.domain.ChatMessage
import com.example.domain.GeneratedImageResult
import com.example.domain.MediaType
import com.example.domain.SavedCreation
import com.example.domain.TextAnalysisResult
import com.example.domain.TextTaskType
import com.example.domain.VaultNote
import com.example.security.CryptoVaultManager
import com.example.security.SecurityPreferences
import com.example.security.SecuritySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class AppTab {
    CHAT, TEXT, MEDIA, SECURITY
}

enum class MediaSubTab {
    IMAGE, VIDEO, GALLERY
}

data class LocalVideoSlide(
    val uriOrPath: String,
    val isFilePath: Boolean,
    val label: String
)

data class BondhuUiState(
    val selectedTab: AppTab = AppTab.CHAT,
    val mediaSubTab: MediaSubTab = MediaSubTab.IMAGE,
    val isLocked: Boolean = false,
    val pinError: String? = null,
    val securitySettings: SecuritySettings = SecuritySettings(),
    val isHardwareKeystore: Boolean = true,
    val angerLevel: Int = 8,
    val mood: AiMood = AiMood.CALM,
    val isAiTyping: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList(),
    val textInput: String = "",
    val selectedTextTask: TextTaskType = TextTaskType.SUMMARIZE,
    val textResult: TextAnalysisResult? = null,
    val isTextProcessing: Boolean = false,
    val imagePrompt: String = "",
    val selectedArtStyle: ArtStyle = ArtStyle.SUNSET_DREAM,
    val selectedArtAspect: ArtAspect = ArtAspect.SQUARE,
    val isGeneratingImage: Boolean = false,
    val latestGeneratedImage: GeneratedImageResult? = null,
    val imageError: String? = null,
    val videoTitle: String = "আমার বাংলা গল্প",
    val videoCaption: String = "অফলাইন বন্ধু এআই দিয়ে তৈরি",
    val selectedVideoSlides: List<LocalVideoSlide> = emptyList(),
    val videoSlideSeconds: Int = 3,
    val isGeneratingVideo: Boolean = false,
    val videoProgress: Float = 0f,
    val videoError: String? = null,
    val activeVideoPlaybackPath: String? = null,
    val savedCreations: List<SavedCreation> = emptyList(),
    val vaultNotes: List<VaultNote> = emptyList(),
    val snackbarMessage: String? = null
)

class BondhuViewModel(
    private val appFilesDir: File,
    private val aiRepository: AiRepository,
    private val vaultRepository: VaultDataRepository,
    private val videoSynthesizer: OfflineVideoSynthesizer,
    private val cryptoVaultManager: CryptoVaultManager,
    private val securityPreferences: SecurityPreferences
) : ViewModel() {

    private val mutableState = MutableStateFlow(BondhuUiState())
    private var lastMessageSentMs: Long = 0L
    private var lastUserMessageText: String? = null
    private var initializedLockCheck = false

    val uiState: StateFlow<BondhuUiState> = combine(
        mutableState,
        vaultRepository.chatMessages,
        vaultRepository.savedCreations,
        vaultRepository.vaultNotes,
        securityPreferences.settingsFlow
    ) { state, messages, creations, notes, secSettings ->
        val mood = AiMood.fromAngerLevel(secSettings.angerLevel)
        val shouldLockInitially = if (!initializedLockCheck) {
            initializedLockCheck = true
            secSettings.isPinEnabled
        } else {
            state.isLocked
        }
        state.copy(
            chatMessages = messages,
            savedCreations = creations,
            vaultNotes = notes,
            securitySettings = secSettings,
            angerLevel = secSettings.angerLevel,
            mood = mood,
            isLocked = shouldLockInitially
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BondhuUiState()
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val hwKeystore = cryptoVaultManager.isUsingHardwareKeystore()
            mutableState.update { it.copy(isHardwareKeystore = hwKeystore) }
        }
        viewModelScope.launch {
            val existing = vaultRepository.chatMessages.first()
            if (existing.isEmpty()) {
                vaultRepository.saveChatMessage(
                    text = "হ্যালো! আমি বন্ধু এআই। আমি সম্পূর্ণ অফলাইনে চলি এবং তোমার ডাটা AES-256 দিয়ে সুরক্ষিত রাখি। তবে বলে রাখি—বেশি ডিস্টার্ব বা তাড়াহুড়ো করলে আমি কিন্তু মানুষের মতোই তাড়াতাড়ি রেগে যাই!",
                    isUser = false,
                    mood = AiMood.CALM,
                    angerLevel = 8,
                    isOfflineEngineNotice = true
                )
            }
        }
    }

    fun selectTab(tab: AppTab) {
        mutableState.update { it.copy(selectedTab = tab) }
    }

    fun selectMediaSubTab(subTab: MediaSubTab) {
        mutableState.update { it.copy(mediaSubTab = subTab) }
    }

    fun consumeSnackbar() {
        mutableState.update { it.copy(snackbarMessage = null) }
    }

    // --- Chat & Anger Engine ---

    fun sendMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || mutableState.value.isAiTyping) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val deltaMs = if (lastMessageSentMs == 0L) 10_000L else (now - lastMessageSentMs)
            lastMessageSentMs = now
            val prevMsg = lastUserMessageText
            lastUserMessageText = text

            val currentAnger = uiState.value.angerLevel
            val currentMood = AiMood.fromAngerLevel(currentAnger)

            vaultRepository.saveChatMessage(
                text = text,
                isUser = true,
                mood = currentMood,
                angerLevel = currentAnger
            )

            mutableState.update { it.copy(isAiTyping = true) }
            val result = aiRepository.chat(
                userMessage = text,
                currentAnger = currentAnger,
                msSinceLastMessage = deltaMs,
                lastUserMessage = prevMsg
            )

            securityPreferences.updateAngerState(result.newAngerLevel, now)
            vaultRepository.saveChatMessage(
                text = result.replyText,
                isUser = false,
                mood = result.newMood,
                angerLevel = result.newAngerLevel,
                isOfflineEngineNotice = result.isOfflinePlaceholder
            )
            mutableState.update { it.copy(isAiTyping = false) }
        }
    }

    fun pokeAiAvatar() {
        if (mutableState.value.isAiTyping) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val currentAnger = uiState.value.angerLevel
            val result = aiRepository.pokeAi(currentAnger)
            securityPreferences.updateAngerState(result.newAngerLevel, now)
            vaultRepository.saveChatMessage(
                text = result.replyText,
                isUser = false,
                mood = result.newMood,
                angerLevel = result.newAngerLevel,
                isOfflineEngineNotice = false
            )
        }
    }

    fun apologizeToAi() {
        viewModelScope.launch {
            val currentAnger = uiState.value.angerLevel
            val result = aiRepository.apologizeToAi(currentAnger)
            securityPreferences.updateAngerState(result.newAngerLevel, System.currentTimeMillis())
            vaultRepository.saveChatMessage(
                text = result.replyText,
                isUser = false,
                mood = result.newMood,
                angerLevel = result.newAngerLevel,
                isOfflineEngineNotice = false
            )
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            vaultRepository.clearChatHistory()
            securityPreferences.updateAngerState(5, System.currentTimeMillis())
            vaultRepository.saveChatMessage(
                text = "সব পুরনো চ্যাট মুছে ফেলা হয়েছে। মনটাও ফ্রেশ! নতুন করে কথা শুরু করো।",
                isUser = false,
                mood = AiMood.CALM,
                angerLevel = 5,
                isOfflineEngineNotice = true
            )
        }
    }

    // --- Offline Text Processing Studio ---

    fun updateTextInput(text: String) {
        mutableState.update { it.copy(textInput = text) }
    }

    fun loadSampleBengaliText() {
        val sample = "বাংলা আমাদের প্রাণের ভাষা। অফলাইন কৃত্রিম বুদ্ধিমত্তা এখন ইন্টারনেট ছাড়াই ফোনের ভেতর টেক্সট বিশ্লেষণ করতে পারে। এতে ব্যক্তিগত তথ্যের গোপনীয়তা ও নিরাপত্তা সম্পূর্ণ সুরক্ষিত থাকে। কোনো তথ্য বাইরের সার্ভারে যায় না বলে ব্যবহারকারীরা নিশ্চিন্তে জরুরি নোট ও বার্তা প্রক্রিয়াকরণ করতে পারেন।"
        mutableState.update { it.copy(textInput = sample) }
    }

    fun runTextTask(taskType: TextTaskType) {
        val input = mutableState.value.textInput
        viewModelScope.launch {
            mutableState.update {
                it.copy(selectedTextTask = taskType, isTextProcessing = true)
            }
            val result = aiRepository.processText(input, taskType)
            mutableState.update {
                it.copy(textResult = result, isTextProcessing = false)
            }
        }
    }

    fun saveProcessedTextToVault() {
        val result = mutableState.value.textResult ?: return
        val original = mutableState.value.textInput.trim()
        if (original.isEmpty()) return

        viewModelScope.launch {
            val title = original.take(28).replace("\n", " ")
            vaultRepository.saveEncryptedVaultNote(
                title = title,
                originalText = original,
                processedText = result.outputText,
                taskLabel = result.taskType.labelBn
            )
            mutableState.update {
                it.copy(snackbarMessage = "AES-256 দিয়ে এনক্রিপ্ট করে ভল্টে সেভ করা হয়েছে!")
            }
        }
    }

    // --- Offline Image & Video Studio ---

    fun updateImagePrompt(prompt: String) {
        mutableState.update { it.copy(imagePrompt = prompt, imageError = null) }
    }

    fun selectArtStyle(style: ArtStyle) {
        mutableState.update { it.copy(selectedArtStyle = style) }
    }

    fun selectArtAspect(aspect: ArtAspect) {
        mutableState.update { it.copy(selectedArtAspect = aspect) }
    }

    fun generateOfflineImage() {
        val prompt = mutableState.value.imagePrompt.trim()
        if (prompt.isEmpty()) {
            mutableState.update { it.copy(imageError = "দয়া করে আগে ছবি সম্পর্কে বাংলায় বা ইংরেজিতে কিছু লিখুন।") }
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            mutableState.update { it.copy(isGeneratingImage = true, imageError = null) }
            try {
                val state = mutableState.value
                val generated = aiRepository.generateArtwork(
                    prompt = prompt,
                    style = state.selectedArtStyle,
                    aspect = state.selectedArtAspect
                )
                vaultRepository.saveMediaCreation(
                    mediaType = MediaType.IMAGE,
                    title = generated.title,
                    prompt = generated.prompt,
                    filePath = generated.filePath
                )
                mutableState.update {
                    it.copy(
                        isGeneratingImage = false,
                        latestGeneratedImage = generated,
                        snackbarMessage = "ছবি তৈরি হয়ে গ্যালারিতে সেভ হয়েছে!"
                    )
                }
            } catch (e: Exception) {
                mutableState.update {
                    it.copy(
                        isGeneratingImage = false,
                        imageError = e.localizedMessage ?: "ছবি তৈরি করতে সমস্যা হয়েছে।"
                    )
                }
            }
        }
    }

    fun useImageInVideoMaker(filePath: String, title: String) {
        val slide = LocalVideoSlide(uriOrPath = filePath, isFilePath = true, label = title)
        mutableState.update { state ->
            val updated = (state.selectedVideoSlides + slide).distinctBy { it.uriOrPath }.take(5)
            state.copy(
                selectedVideoSlides = updated,
                mediaSubTab = MediaSubTab.VIDEO,
                snackbarMessage = "ছবিটি ভিডিও মেকারে যুক্ত হয়েছে!"
            )
        }
    }

    fun addPickedPhotosToVideo(uris: List<String>) {
        if (uris.isEmpty()) return
        mutableState.update { state ->
            val newSlides = uris.mapIndexed { idx, uri ->
                LocalVideoSlide(
                    uriOrPath = uri,
                    isFilePath = false,
                    label = "ছবি ${BengaliTextProcessor.toBengaliDigits(state.selectedVideoSlides.size + idx + 1)}"
                )
            }
            val merged = (state.selectedVideoSlides + newSlides).distinctBy { it.uriOrPath }.take(5)
            state.copy(selectedVideoSlides = merged)
        }
    }

    fun removeVideoSlide(uriOrPath: String) {
        mutableState.update { state ->
            state.copy(selectedVideoSlides = state.selectedVideoSlides.filterNot { it.uriOrPath == uriOrPath })
        }
    }

    fun updateVideoTitle(title: String) {
        mutableState.update { it.copy(videoTitle = title) }
    }

    fun updateVideoCaption(caption: String) {
        mutableState.update { it.copy(videoCaption = caption) }
    }

    fun setVideoSlideDuration(seconds: Int) {
        mutableState.update { it.copy(videoSlideSeconds = seconds.coerceIn(2, 5)) }
    }

    fun generateMp4Video() {
        val state = mutableState.value
        if (state.isGeneratingVideo) return
        viewModelScope.launch {
            mutableState.update {
                it.copy(isGeneratingVideo = true, videoProgress = 0.05f, videoError = null)
            }
            try {
                val slides = state.selectedVideoSlides.map {
                    VideoSlideInput(
                        uriOrPath = it.uriOrPath,
                        isFilePath = it.isFilePath,
                        caption = state.videoCaption
                    )
                }
                val result = videoSynthesizer.createMp4Video(
                    title = state.videoTitle,
                    slides = slides,
                    secondsPerSlide = state.videoSlideSeconds
                ) { progress ->
                    mutableState.update { it.copy(videoProgress = progress) }
                }

                vaultRepository.saveMediaCreation(
                    mediaType = MediaType.VIDEO,
                    title = result.title,
                    prompt = state.videoCaption,
                    filePath = result.filePath,
                    durationSeconds = result.durationSeconds
                )

                mutableState.update {
                    it.copy(
                        isGeneratingVideo = false,
                        videoProgress = 1f,
                        activeVideoPlaybackPath = result.filePath,
                        snackbarMessage = "অফলাইন MP4 ভিডিও তৈরি সম্পন্ন হয়েছে!"
                    )
                }
            } catch (e: Exception) {
                mutableState.update {
                    it.copy(
                        isGeneratingVideo = false,
                        videoError = "ভিডিও তৈরি ব্যর্থ হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}"
                    )
                }
            }
        }
    }

    fun openVideoPlayer(filePath: String?) {
        mutableState.update { it.copy(activeVideoPlaybackPath = filePath) }
    }

    fun deleteCreation(creation: SavedCreation) {
        viewModelScope.launch {
            vaultRepository.deleteCreation(creation)
            if (mutableState.value.activeVideoPlaybackPath == creation.filePath) {
                mutableState.update { it.copy(activeVideoPlaybackPath = null) }
            }
            mutableState.update { it.copy(snackbarMessage = "আইটেমটি মুছে ফেলা হয়েছে।") }
        }
    }

    // --- Data Security & PIN Lock ---

    fun unlockWithPin(pin: String) {
        val settings = uiState.value.securitySettings
        if (cryptoVaultManager.verifyPin(pin, settings.pinSalt, settings.pinHash)) {
            mutableState.update { it.copy(isLocked = false, pinError = null) }
        } else {
            mutableState.update { it.copy(pinError = "পিন ভুল হয়েছে! আবার চেষ্টা করুন।") }
        }
    }

    fun setupNewPin(pin: String) {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return
        viewModelScope.launch {
            val salt = cryptoVaultManager.generateSalt()
            val hash = cryptoVaultManager.hashPin(pin, salt)
            securityPreferences.savePin(hash, salt)
            mutableState.update {
                it.copy(snackbarMessage = "৪-সংখ্যার নিরাপত্তা পিন চালু হয়েছে!")
            }
        }
    }

    fun disablePin() {
        viewModelScope.launch {
            securityPreferences.disablePin()
            mutableState.update {
                it.copy(isLocked = false, snackbarMessage = "অ্যাপ পিন লক বন্ধ করা হয়েছে।")
            }
        }
    }

    fun lockAppNow() {
        if (uiState.value.securitySettings.isPinEnabled) {
            mutableState.update { it.copy(isLocked = true, pinError = null) }
        } else {
            mutableState.update {
                it.copy(snackbarMessage = "লক করতে আগে সুরক্ষা ট্যাব থেকে ৪-সংখ্যার পিন সেট করুন।")
            }
        }
    }

    fun toggleScreenPrivacy(enabled: Boolean) {
        viewModelScope.launch {
            securityPreferences.setScreenPrivacy(enabled)
        }
    }

    fun deleteVaultNote(id: Long) {
        viewModelScope.launch {
            vaultRepository.deleteVaultNote(id)
        }
    }

    fun wipeAllData() {
        viewModelScope.launch {
            vaultRepository.wipeAllData(appFilesDir)
            securityPreferences.clearAll()
            mutableState.update {
                it.copy(
                    isLocked = false,
                    textResult = null,
                    latestGeneratedImage = null,
                    selectedVideoSlides = emptyList(),
                    activeVideoPlaybackPath = null,
                    snackbarMessage = "সকল গোপন ডাটা স্থায়ীভাবে মুছে ফেলা হয়েছে!"
                )
            }
            vaultRepository.saveChatMessage(
                text = "সব ডাটা নিরাপদে মুছে ফেলা হয়েছে। আমি একদম নতুন অবস্থায় আছি!",
                isUser = false,
                mood = AiMood.CALM,
                angerLevel = 0,
                isOfflineEngineNotice = true
            )
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val crypto = CryptoVaultManager()
                    val db = BondhuDatabase.getInstance(appContext)
                    val vaultRepo = VaultDataRepository(db.bondhuDao(), crypto)
                    val textProcessor = BengaliTextProcessor(crypto)
                    val artSynth = OfflineArtSynthesizer(appContext)
                    val videoSynth = OfflineVideoSynthesizer(appContext)
                    val aiRepo = OfflineAiEngineRepository(textProcessor, artSynth)
                    val secPrefs = SecurityPreferences(appContext)
                    return BondhuViewModel(
                        appFilesDir = appContext.filesDir,
                        aiRepository = aiRepo,
                        vaultRepository = vaultRepo,
                        videoSynthesizer = videoSynth,
                        cryptoVaultManager = crypto,
                        securityPreferences = secPrefs
                    ) as T
                }
            }
    }
}
