package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BondhuDao {

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    fun observeChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatMessages()

    @Query("SELECT * FROM saved_creations ORDER BY createdAt DESC, id DESC")
    fun observeSavedCreations(): Flow<List<SavedCreationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedCreation(creation: SavedCreationEntity): Long

    @Query("DELETE FROM saved_creations WHERE id = :id")
    suspend fun deleteSavedCreation(id: Long)

    @Query("DELETE FROM saved_creations")
    suspend fun clearSavedCreations()

    @Query("SELECT * FROM vault_notes ORDER BY createdAt DESC, id DESC")
    fun observeVaultNotes(): Flow<List<VaultNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultNote(note: VaultNoteEntity): Long

    @Query("DELETE FROM vault_notes WHERE id = :id")
    suspend fun deleteVaultNote(id: Long)

    @Query("DELETE FROM vault_notes")
    suspend fun clearVaultNotes()
}
