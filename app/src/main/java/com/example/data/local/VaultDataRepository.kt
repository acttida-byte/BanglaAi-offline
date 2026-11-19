package com.example.data.local

import com.example.domain.AiMood
import com.example.domain.ChatMessage
import com.example.domain.MediaType
import com.example.domain.SavedCreation
import com.example.domain.VaultNote
import com.example.security.CryptoVaultManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class VaultDataRepository(
    private val dao: BondhuDao,
    private val cryptoVaultManager: CryptoVaultManager
) {

    val chatMessages: Flow<List<ChatMessage>> = dao.observeChatMessages().map { list ->
        list.map { entity ->
            ChatMessage(
                id = entity.id,
                text = cryptoVaultManager.decrypt(entity.encryptedContent),
                isUser = entity.isUser,
                mood = runCatching { AiMood.valueOf(entity.moodTag) }.getOrDefault(AiMood.CALM),
                angerLevel = entity.angerAtTime,
                isOfflineEngineNotice = entity.isPlaceholderNotice,
                timestamp = entity.timestamp
            )
        }
    }

    val savedCreations: Flow<List<SavedCreation>> = dao.observeSavedCreations().map { list ->
        list.map { entity ->
            SavedCreation(
                id = entity.id,
                mediaType = if (entity.mediaType == "VIDEO") MediaType.VIDEO else MediaType.IMAGE,
                title = entity.title,
                prompt = entity.prompt,
                filePath = entity.filePath,
                durationSeconds = entity.durationSeconds,
                createdAt = entity.createdAt
            )
        }
    }

    val vaultNotes: Flow<List<VaultNote>> = dao.observeVaultNotes().map { list ->
        list.map { entity ->
            VaultNote(
                id = entity.id,
                title = entity.title,
                originalText = cryptoVaultManager.decrypt(entity.encryptedOriginal),
                processedText = cryptoVaultManager.decrypt(entity.encryptedProcessed),
                encryptedPreview = entity.encryptedProcessed.take(44) + "...",
                taskLabel = entity.taskLabel,
                createdAt = entity.createdAt
            )
        }
    }

    suspend fun saveChatMessage(
        text: String,
        isUser: Boolean,
        mood: AiMood,
        angerLevel: Int,
        isOfflineEngineNotice: Boolean = false
    ) {
        dao.insertChatMessage(
            ChatMessageEntity(
                encryptedContent = cryptoVaultManager.encrypt(text),
                isUser = isUser,
                moodTag = mood.name,
                angerAtTime = angerLevel,
                isPlaceholderNotice = isOfflineEngineNotice
            )
        )
    }

    suspend fun clearChatHistory() {
        dao.clearChatMessages()
    }

    suspend fun saveMediaCreation(
        mediaType: MediaType,
        title: String,
        prompt: String,
        filePath: String,
        durationSeconds: Int = 0
    ): Long {
        return dao.insertSavedCreation(
            SavedCreationEntity(
                mediaType = mediaType.name,
                title = title,
                prompt = prompt,
                filePath = filePath,
                durationSeconds = durationSeconds
            )
        )
    }

    suspend fun deleteCreation(creation: SavedCreation) {
        runCatching { File(creation.filePath).delete() }
        dao.deleteSavedCreation(creation.id)
    }

    suspend fun saveEncryptedVaultNote(
        title: String,
        originalText: String,
        processedText: String,
        taskLabel: String
    ): Long {
        return dao.insertVaultNote(
            VaultNoteEntity(
                title = title,
                encryptedOriginal = cryptoVaultManager.encrypt(originalText),
                encryptedProcessed = cryptoVaultManager.encrypt(processedText),
                taskLabel = taskLabel
            )
        )
    }

    suspend fun deleteVaultNote(id: Long) {
        dao.deleteVaultNote(id)
    }

    suspend fun wipeAllData(filesDir: File) {
        dao.clearChatMessages()
        dao.clearSavedCreations()
        dao.clearVaultNotes()
        runCatching { File(filesDir, "images").deleteRecursively() }
        runCatching { File(filesDir, "videos").deleteRecursively() }
    }
}
