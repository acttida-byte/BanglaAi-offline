package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatMessageEntity::class,
        SavedCreationEntity::class,
        VaultNoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BondhuDatabase : RoomDatabase() {
    abstract fun bondhuDao(): BondhuDao

    companion object {
        @Volatile
        private var INSTANCE: BondhuDatabase? = null

        fun getInstance(context: Context): BondhuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BondhuDatabase::class.java,
                    "bondhu_offline_vault.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
