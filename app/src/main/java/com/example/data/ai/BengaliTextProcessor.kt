package com.example.data.ai

import com.example.domain.TextAnalysisResult
import com.example.domain.TextTaskType
import com.example.security.CryptoVaultManager
import kotlin.math.ceil

class BengaliTextProcessor(private val cryptoVaultManager: CryptoVaultManager) {

    private val bengaliStopWords = setOf(
        "এবং", "ও", "আর", "কিন্তু", "তবে", "যে", "এই", "সেই", "তা", "তাই", "করা", "করে",
        "হয়", "হয়", "হয়েছে", "হয়েছে", "আছে", "ছিল", "থেকে", "জন্য", "দিয়ে", "দিয়ে",
        "নিয়ে", "নিয়ে", "সাথে", "একটি", "একটা", "কোনো", "কিছু", "আমি", "তুমি", "আপনি",
        "সে", "তারা", "আমরা", "না", "হ্যাঁ", "বা", "যদি", "তাহলে", "মধ্যে", "উপর", "নিচে",
        "the", "is", "in", "at", "of", "and", "to", "a", "for", "with", "on", "this", "that"
    )

    private val positiveWords = setOf(
        "ভালো", "দারুণ", "সুন্দর", "আনন্দ", "খুশি", "অসাধারণ", "ধন্যবাদ", "সফল", "ভালোবাসা",
        "শান্ত", "আশা", "চমৎকার", "প্রিয়", "প্রিয়", "মজার", "good", "great", "happy", "love", "nice"
    )

    private val negativeWords = setOf(
        "খারাপ", "রাগ", "বিরক্ত", "দুঃখ", "কষ্ট", "বাজে", "বোকা", "ফালতু", "অসহ্য", "কান্না",
        "ভুল", "সমস্যা", "বিপদ", "ব্যর্থ", "বিরক্তি", "angry", "bad", "sad", "hate", "annoying"
    )

    fun process(inputText: String, taskType: TextTaskType): TextAnalysisResult {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) {
            return TextAnalysisResult(
                outputText = "অনুগ্রহ করে প্রসেস করার জন্য কিছু টেক্সট লিখুন।",
                wordCount = 0,
                charCount = 0,
                sentenceCount = 0,
                readingSeconds = 0,
                sentimentLabelBn = "নিরপেক্ষ",
                keywords = emptyList(),
                taskType = taskType
            )
        }

        val words = extractWords(trimmed)
        val sentences = splitSentences(trimmed)
        val keywords = extractTopKeywords(words, limit = 5)
        val sentiment = detectSentiment(words)
        val readingSec = ceil(words.size / 3.0).toInt().coerceAtLeast(1)

        val output = when (taskType) {
            TextTaskType.SUMMARIZE -> buildSummary(sentences, keywords)
            TextTaskType.ANALYZE -> buildDeepAnalysis(trimmed, words, sentences, keywords, sentiment, readingSec)
            TextTaskType.KEY_POINTS -> buildKeyPoints(sentences, keywords)
            TextTaskType.ENCRYPT -> cryptoVaultManager.encrypt(trimmed)
            TextTaskType.DECRYPT -> cryptoVaultManager.decrypt(trimmed)
            TextTaskType.FORMAT -> formatBengaliText(trimmed)
        }

        return TextAnalysisResult(
            outputText = output,
            wordCount = words.size,
            charCount = trimmed.length,
            sentenceCount = sentences.size,
            readingSeconds = readingSec,
            sentimentLabelBn = sentiment,
            keywords = keywords,
            taskType = taskType
        )
    }

    private fun extractWords(text: String): List<String> {
        return text.split(Regex("[\\s,।.?!;:\"'()\\[\\]{}]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun splitSentences(text: String): List<String> {
        return text.split(Regex("(?<=[।?!.\\n])+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun extractTopKeywords(words: List<String>, limit: Int): List<String> {
        val freq = mutableMapOf<String, Int>()
        for (word in words) {
            val clean = word.lowercase()
            if (clean.length >= 2 && clean !in bengaliStopWords) {
                freq[clean] = (freq[clean] ?: 0) + 1
            }
        }
        return freq.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }

    private fun detectSentiment(words: List<String>): String {
        var score = 0
        for (w in words) {
            val lower = w.lowercase()
            if (positiveWords.any { lower.contains(it) }) score++
            if (negativeWords.any { lower.contains(it) }) score--
        }
        return when {
            score >= 2 -> "ইতিবাচক ও প্রাণবন্ত"
            score == 1 -> "হালকা ইতিবাচক"
            score <= -2 -> "নেতিবাচক বা উত্তেজিত"
            score == -1 -> "একটু চিন্তিত / নেতিবাচক"
            else -> "তথ্যমূলক ও নিরপেক্ষ"
        }
    }

    private fun buildSummary(sentences: List<String>, keywords: List<String>): String {
        if (sentences.size <= 2) {
            return "সংক্ষিপ্ত সারাংশ:\n${sentences.joinToString(" ")}"
        }
        val keywordSet = keywords.toSet()
        val scored = sentences.mapIndexed { idx, sentence ->
            val wordsInSentence = extractWords(sentence)
            val kwMatches = wordsInSentence.count { it.lowercase() in keywordSet }
            val positionBonus = if (idx == 0) 2 else if (idx == sentences.lastIndex) 1 else 0
            val lengthPenalty = if (wordsInSentence.size < 3) -2 else 0
            Triple(idx, sentence, kwMatches * 2 + positionBonus + lengthPenalty)
        }
        val targetCount = (sentences.size / 2).coerceIn(2, 4)
        val topSentences = scored
            .sortedByDescending { it.third }
            .take(targetCount)
            .sortedBy { it.first }
            .map { it.second }

        return "মূল সারাংশ (${toBengaliDigits(topSentences.size)}টি বাক্যে):\n\n" +
            topSentences.joinToString(" ")
    }

    private fun buildDeepAnalysis(
        text: String,
        words: List<String>,
        sentences: List<String>,
        keywords: List<String>,
        sentiment: String,
        readingSec: Int
    ): String {
        val bengaliChars = text.count { it in '\u0980'..'\u09FF' }
        val latinChars = text.count { it in 'a'..'z' || it in 'A'..'Z' }
        val languageMix = when {
            bengaliChars > latinChars * 2 -> "প্রধানত বাংলা"
            latinChars > bengaliChars * 2 -> "প্রধানত ইংরেজি"
            else -> "বাংলা ও ইংরেজি মিশ্র"
        }
        val kwText = if (keywords.isEmpty()) "উল্লেখযোগ্য কীওয়ার্ড নেই" else keywords.joinToString(" • ")

        return buildString {
            appendLine("অফলাইন টেক্সট বিশ্লেষণ রিপোর্ট:")
            appendLine("• ভাষা: $languageMix")
            appendLine("• অনুভূতি (Tone): $sentiment")
            appendLine("• মোট শব্দ: ${toBengaliDigits(words.size)}টি")
            appendLine("• মোট বাক্য: ${toBengaliDigits(sentences.size)}টি")
            appendLine("• আনুমানিক পড়ার সময়: ${toBengaliDigits(readingSec)} সেকেন্ড")
            append("• মূল কীওয়ার্ড: $kwText")
        }
    }

    private fun buildKeyPoints(sentences: List<String>, keywords: List<String>): String {
        val points = sentences.take(6).mapIndexed { index, s ->
            "${toBengaliDigits(index + 1)}। $s"
        }
        return buildString {
            appendLine("গুরুত্বপূর্ণ পয়েন্টসমূহ:")
            appendLine()
            points.forEach { appendLine(it) }
            if (keywords.isNotEmpty()) {
                appendLine()
                append("মূল ট্যাগ: ${keywords.joinToString(", ")}")
            }
        }
    }

    private fun formatBengaliText(text: String): String {
        return text
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\s+([।,?!;:])"), "$1")
            .replace(Regex("([।?!])([^\\s\\n])"), "$1 $2")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    companion object {
        fun toBengaliDigits(number: Int): String = toBengaliDigits(number.toString())

        fun toBengaliDigits(input: String): String {
            val map = mapOf(
                '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
                '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
            )
            return input.map { map[it] ?: it }.joinToString("")
        }
    }
}
