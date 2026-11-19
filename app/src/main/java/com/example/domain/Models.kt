package com.example.domain

enum class AiMood(val labelBn: String, val emojiTag: String) {
    CALM("শান্ত ও হাসিখুশি", "শান্ত"),
    ANNOYED("একটু বিরক্ত", "বিরক্ত"),
    ANGRY("মেজাজ গরম!", "রাগান্বিত"),
    FURIOUS("চরম ক্ষিপ্ত — কথা বন্ধ!", "ক্ষিপ্ত");

    companion object {
        fun fromAngerLevel(level: Int): AiMood = when {
            level < 25 -> CALM
            level < 55 -> ANNOYED
            level < 85 -> ANGRY
            else -> FURIOUS
        }
    }
}

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
    val mood: AiMood,
    val angerLevel: Int,
    val isOfflineEngineNotice: Boolean,
    val timestamp: Long
)

data class AiChatResult(
    val replyText: String,
    val newAngerLevel: Int,
    val newMood: AiMood,
    val isOfflinePlaceholder: Boolean = true
)

enum class TextTaskType(val labelBn: String, val subtitleBn: String) {
    SUMMARIZE("সারাংশ তৈরি", "মূল বক্তব্য সংক্ষেপে বের করবে"),
    ANALYZE("গভীর বিশ্লেষণ", "শব্দ, অনুভূতি ও মূল শব্দ খুঁজবে"),
    KEY_POINTS("মূল পয়েন্ট", "পয়েন্ট আকারে সাজিয়ে দেবে"),
    ENCRYPT("গোপন এনক্রিপশন", "AES-256 কোডে সুরক্ষিত করবে"),
    DECRYPT("ডিক্রিপ্ট করুন", "সুরক্ষিত কোড থেকে টেক্সট ফিরিয়ে আনবে"),
    FORMAT("সুন্দর ফরম্যাটিং", "দাড়ি, স্পেস ও প্যারা ঠিক করবে")
}

data class TextAnalysisResult(
    val outputText: String,
    val wordCount: Int,
    val charCount: Int,
    val sentenceCount: Int,
    val readingSeconds: Int,
    val sentimentLabelBn: String,
    val keywords: List<String>,
    val taskType: TextTaskType,
    val isOfflineLocalProcessing: Boolean = true
)

enum class ArtStyle(val labelBn: String) {
    WATERCOLOR("জলরঙ"),
    NEON_CYBER("নিয়ন ফিউচার"),
    MINIMAL_VECTOR("মিনিমাল আর্ট"),
    SUNSET_DREAM("স্বপ্নীল প্রকৃতি")
}

enum class ArtAspect(val labelBn: String, val width: Int, val height: Int) {
    SQUARE("১:১ বর্গাকার", 1024, 1024),
    LANDSCAPE("১৬:৯ ওয়াইড", 1280, 720),
    PORTRAIT("৩:৪ পোর্ট্রেট", 768, 1024)
}

data class GeneratedImageResult(
    val filePath: String,
    val title: String,
    val prompt: String,
    val isOfflineArtwork: Boolean = true
)

enum class MediaType {
    IMAGE, VIDEO
}

data class SavedCreation(
    val id: Long,
    val mediaType: MediaType,
    val title: String,
    val prompt: String,
    val filePath: String,
    val durationSeconds: Int,
    val createdAt: Long
)

data class VaultNote(
    val id: Long,
    val title: String,
    val originalText: String,
    val processedText: String,
    val encryptedPreview: String,
    val taskLabel: String,
    val createdAt: Long
)
