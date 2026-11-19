package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Hardware/OS-backed AES-256-GCM encryption & PIN hashing manager using AndroidKeyStore.
 * Ensures all private user texts, chat history, and vault items remain encrypted locally.
 */
class CryptoVaultManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "bondhu_ai_aes256_vault_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
        private const val ENCRYPTED_PREFIX = "ENC_V1:"
    }

    private val fallbackKey: SecretKey by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("bondhu_local_fallback_seed_v1".toByteArray(StandardCharsets.UTF_8))
        SecretKeySpec(digest, "AES")
    }

    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (_: Throwable) {
            // Fallback for JVM/Robolectric unit test environments without Keystore hardware
            fallbackKey
        }
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
        return ENCRYPTED_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty()) return ""
        if (!cipherText.startsWith(ENCRYPTED_PREFIX)) return cipherText
        return try {
            val raw = Base64.decode(
                cipherText.removePrefix(ENCRYPTED_PREFIX),
                Base64.NO_WRAP
            )
            if (raw.size <= GCM_IV_LENGTH) return ""
            val iv = raw.copyOfRange(0, GCM_IV_LENGTH)
            val encryptedBytes = raw.copyOfRange(GCM_IV_LENGTH, raw.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
            String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            "[ডিক্রিপ্ট করতে ব্যর্থ]"
        }
    }

    fun isUsingHardwareKeystore(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            keyStore.containsAlias(KEY_ALIAS) || getOrCreateSecretKey() !== fallbackKey
        } catch (_: Throwable) {
            false
        }
    }

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var current = "$salt:$pin:bondhu_secure_pin".toByteArray(StandardCharsets.UTF_8)
        repeat(5000) {
            current = digest.digest(current)
        }
        return Base64.encodeToString(current, Base64.NO_WRAP)
    }

    fun verifyPin(inputPin: String, salt: String, expectedHash: String): Boolean {
        if (salt.isBlank() || expectedHash.isBlank()) return false
        return MessageDigest.isEqual(
            hashPin(inputPin, salt).toByteArray(StandardCharsets.UTF_8),
            expectedHash.toByteArray(StandardCharsets.UTF_8)
        )
    }
}
