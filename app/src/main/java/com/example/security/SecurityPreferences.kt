package com.example.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bondhu_security_prefs"
)

data class SecuritySettings(
    val isPinEnabled: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = "",
    val isScreenPrivacyEnabled: Boolean = false,
    val angerLevel: Int = 8,
    val lastDisturbEpochMs: Long = 0L
)

class SecurityPreferences(private val context: Context) {

    private object Keys {
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val SCREEN_PRIVACY = booleanPreferencesKey("screen_privacy")
        val ANGER_LEVEL = intPreferencesKey("anger_level")
        val LAST_DISTURB_MS = longPreferencesKey("last_disturb_ms")
    }

    val settingsFlow: Flow<SecuritySettings> = context.securityDataStore.data.map { prefs ->
        SecuritySettings(
            isPinEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            pinSalt = prefs[Keys.PIN_SALT] ?: "",
            isScreenPrivacyEnabled = prefs[Keys.SCREEN_PRIVACY] ?: false,
            angerLevel = prefs[Keys.ANGER_LEVEL] ?: 8,
            lastDisturbEpochMs = prefs[Keys.LAST_DISTURB_MS] ?: 0L
        )
    }

    suspend fun savePin(pinHash: String, pinSalt: String) {
        context.securityDataStore.edit { prefs ->
            prefs[Keys.PIN_ENABLED] = true
            prefs[Keys.PIN_HASH] = pinHash
            prefs[Keys.PIN_SALT] = pinSalt
        }
    }

    suspend fun disablePin() {
        context.securityDataStore.edit { prefs ->
            prefs[Keys.PIN_ENABLED] = false
            prefs[Keys.PIN_HASH] = ""
            prefs[Keys.PIN_SALT] = ""
        }
    }

    suspend fun setScreenPrivacy(enabled: Boolean) {
        context.securityDataStore.edit { prefs ->
            prefs[Keys.SCREEN_PRIVACY] = enabled
        }
    }

    suspend fun updateAngerState(angerLevel: Int, lastDisturbEpochMs: Long) {
        context.securityDataStore.edit { prefs ->
            prefs[Keys.ANGER_LEVEL] = angerLevel.coerceIn(0, 100)
            prefs[Keys.LAST_DISTURB_MS] = lastDisturbEpochMs
        }
    }

    suspend fun clearAll() {
        context.securityDataStore.edit { it.clear() }
    }
}
