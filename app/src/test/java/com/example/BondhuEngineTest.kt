package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.BengaliTextProcessor
import com.example.data.ai.OfflineAiEngineRepository
import com.example.data.ai.OfflineArtSynthesizer
import com.example.domain.AiMood
import com.example.domain.ArtAspect
import com.example.domain.ArtStyle
import com.example.domain.TextTaskType
import com.example.security.CryptoVaultManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BondhuEngineTest {

    @Test
    fun cryptoVault_encryptsAndDecryptsBengaliTextAndVerifiesPin() {
        val crypto = CryptoVaultManager()
        val secret = "আমার গোপন বাংলা নোট ১২৩"
        val encrypted = crypto.encrypt(secret)

        assertNotEquals(secret, encrypted)
        assertTrue(encrypted.startsWith("ENC_V1:"))
        assertEquals(secret, crypto.decrypt(encrypted))

        val salt = crypto.generateSalt()
        val hash = crypto.hashPin("4829", salt)
        assertTrue(crypto.verifyPin("4829", salt, hash))
        assertFalse(crypto.verifyPin("0000", salt, hash))
    }

    @Test
    fun bengaliTextProcessor_summarizesAndAnalyzesAccurately() {
        val processor = BengaliTextProcessor(CryptoVaultManager())
        val sample = "বাংলা একটি মধুর ভাষা। অফলাইন এআই ইন্টারনেট ছাড়াই দারুণ কাজ করে। ডাটা সিকিউরিটি সবসময় গুরুত্বপূর্ণ।"

        val summary = processor.process(sample, TextTaskType.SUMMARIZE)
        assertEquals(3, summary.sentenceCount)
        assertTrue(summary.wordCount >= 12)
        assertTrue(summary.outputText.contains("সারাংশ"))

        val encrypted = processor.process(sample, TextTaskType.ENCRYPT)
        val decrypted = processor.process(encrypted.outputText, TextTaskType.DECRYPT)
        assertEquals(sample, decrypted.outputText)
    }

    @Test
    fun angerEngine_escalatesRapidlyOnDisturbanceAndCalmsOnApology() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = OfflineAiEngineRepository(
            BengaliTextProcessor(CryptoVaultManager()),
            OfflineArtSynthesizer(context)
        )

        val first = repo.chat("এই তাড়াতাড়ি উত্তর দাও!!", currentAnger = 10, msSinceLastMessage = 1500L, lastUserMessage = null)
        assertTrue(first.newAngerLevel >= 50)

        val poked = repo.pokeAi(first.newAngerLevel)
        val pokedTwice = repo.pokeAi(poked.newAngerLevel)
        assertEquals(AiMood.FURIOUS, pokedTwice.newMood)

        val calmed = repo.apologizeToAi(pokedTwice.newAngerLevel)
        assertTrue(calmed.newAngerLevel < pokedTwice.newAngerLevel)
    }

    @Test
    fun offlineArtSynthesizer_createsValidPngFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val synth = OfflineArtSynthesizer(context)
        val result = synth.generateArtwork("পাহাড়ে সুন্দর সূর্যাস্ত", ArtStyle.SUNSET_DREAM, ArtAspect.SQUARE)
        val file = File(result.filePath)
        assertTrue(file.exists())
        assertTrue(file.length() > 1000L)
        file.delete()
    }
}
