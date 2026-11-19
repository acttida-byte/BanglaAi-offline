package com.example.domain

/**
 * Unified AI interface routing all generative/AI capabilities.
 * Currently backed by the 100% offline local engine (OfflineAiEngineRepository)
 * since cloud Firebase AI Logic was declined.
 */
interface AiRepository {
    suspend fun chat(
        userMessage: String,
        currentAnger: Int,
        msSinceLastMessage: Long,
        lastUserMessage: String?
    ): AiChatResult

    suspend fun pokeAi(currentAnger: Int): AiChatResult

    suspend fun apologizeToAi(currentAnger: Int): AiChatResult

    suspend fun processText(
        inputText: String,
        taskType: TextTaskType
    ): TextAnalysisResult

    suspend fun generateArtwork(
        prompt: String,
        style: ArtStyle,
        aspect: ArtAspect
    ): GeneratedImageResult
}
