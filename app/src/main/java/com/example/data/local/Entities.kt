package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val encryptedContent: String,
    val isUser: Boolean,
    val moodTag: String,
    val angerAtTime: Int,
    val isPlaceholderNotice: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_creations")
data class SavedCreationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaType: String, // "IMAGE" or "VIDEO"
    val title: String,
    val prompt: String,
    val filePath: String,
    val durationSeconds: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_notes")
data class VaultNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val encryptedOriginal: String,
    val encryptedProcessed: String,
    val taskLabel: String,
    val createdAt: Long = System.currentTimeMillis()
)
