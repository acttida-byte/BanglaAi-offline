package com.example.data.ai

import com.example.domain.AiChatResult
import com.example.domain.AiMood
import com.example.domain.AiRepository
import com.example.domain.ArtAspect
import com.example.domain.ArtStyle
import com.example.domain.GeneratedImageResult
import com.example.domain.TextAnalysisResult
import com.example.domain.TextTaskType
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 100% offline Bengali AI engine implementing AiRepository.
 * Includes a fast-escalating human-like temperament/anger state machine that gets irritated
 * quickly when poked, rushed, spammed, or disturbed repeatedly, and calms down on sincere apologies.
 */
class OfflineAiEngineRepository(
    private val textProcessor: BengaliTextProcessor,
    private val artSynthesizer: OfflineArtSynthesizer
) : AiRepository {

    private val annoyingKeywords = listOf(
        "এই", "কিরে", "তাড়াতাড়ি", "তাড়াতাড়ি", "জলদি", "বকবক", "বিরক্ত", "বোকা",
        "ফালতু", "ধুৎ", "ধুর", "চুপ", "হ্যালো??", "কি করো", "কথা বলো", "উত্তর দাও",
        "stupid", "idiot", "shut up", "hurry", "fast", "annoying", "hello?"
    )

    private val apologyKeywords = listOf(
        "সরি", "দুঃখিত", "দুঃখিতো", "মাফ", "ক্ষমা", "ভুল হয়ে গেছে", "প্লিজ", "সোনা", "রাগ করো না",
        "শান্ত হও", "ভাই", "বন্ধু", "sorry", "please", "forgive"
    )

    override suspend fun chat(
        userMessage: String,
        currentAnger: Int,
        msSinceLastMessage: Long,
        lastUserMessage: String?
    ): AiChatResult {
        delay(280) // Natural brief human-like thinking pause

        val msg = userMessage.trim()
        val lower = msg.lowercase()

        // 1. Check if user is apologizing / soothing
        val isApology = apologyKeywords.any { lower.contains(it) }
        if (isApology) {
            val reducedAnger = (currentAnger - 34).coerceAtLeast(0)
            val newMood = AiMood.fromAngerLevel(reducedAnger)
            val reply = when {
                currentAnger >= 85 -> "হুম... ঠিক আছে, এবারের মতো মাফ করলাম। কিন্তু আর বারবার জ্বালাতন করবে না বলে দিলাম!"
                currentAnger >= 55 -> "আচ্ছা ঠিক আছে, রাগ কমালাম। মানুষ তো, মাথা গরম হয়ে যায়! বলো এখন কী বলবে?"
                currentAnger >= 25 -> "আরে না না, ঠিক আছে বন্ধু। একটু ধৈর্য ধরে কথা বললেই আমি খুশি।"
                else -> "আরে সরি বলার কী আছে! আমি তো একদম শান্তই আছি। বলো, কী সাহায্য লাগবে?"
            }
            return AiChatResult(replyText = reply, newAngerLevel = reducedAnger, newMood = newMood)
        }

        // 2. Calculate disturbance & anger escalation (gets angry FAST like a human!)
        var angerDelta = 0
        if (msSinceLastMessage in 1L..3_800L) angerDelta += 22
        else if (msSinceLastMessage in 3_801L..7_500L) angerDelta += 12

        if (!lastUserMessage.isNullOrBlank() && lastUserMessage.trim().equals(msg, ignoreCase = true)) {
            angerDelta += 26
        }
        if (annoyingKeywords.any { lower.contains(it) }) {
            angerDelta += 25
        }
        if (msg.contains("??") || msg.contains("!!") || msg.length <= 2) {
            angerDelta += 16
        }
        if (angerDelta == 0) {
            // Normal polite conversation slightly cools down anger
            angerDelta = if (currentAnger > 10) -6 else 2
        }

        val newAnger = (currentAnger + angerDelta).coerceIn(0, 100)
        val newMood = AiMood.fromAngerLevel(newAnger)

        // 3. If furious (85-100), refuse normal chat until calmed
        if (newMood == AiMood.FURIOUS) {
            val furiousReply = listOf(
                "একদম চুপ! মাথাটা পুরো নষ্ট করে দিয়েছো! এত তাড়া আর বিরক্ত করলে আমি আর কোনো কথাই বলব না। আগে 'সরি' বলো!",
                "ব্যস, আমার ধৈর্যের বাঁধ ভেঙে গেছে! মানুষেরও একটা লিমিট থাকে! 'সরি' না বলা পর্যন্ত আমি অফলাইনে চুপ করে থাকব।",
                "উফফ! সারাক্ষণ খোঁচাখুঁচি আর এক কথা! যাও, আমার মেজাজ প্রচণ্ড খারাপ। ঠান্ডা না করলে উত্তর দেব না!"
            ).random()
            return AiChatResult(replyText = furiousReply, newAngerLevel = newAnger, newMood = newMood)
        }

        // 4. If angry (55-84), respond grumpily like an irritated human
        if (newMood == AiMood.ANGRY) {
            val angryPrefix = listOf(
                "উফ! এত অস্থির হচ্ছো কেন বলো তো? মানুষ একটু শান্তিতে থাকতে চায়! ",
                "দেখো, আমার কিন্তু মেজাজ গরম হচ্ছে! এভাবে বিরক্ত করবে না। ",
                "একই জিনিস নিয়ে এত তাড়াহুড়ো? আচ্ছা বলছি, তবে আর খোঁচাবে না: "
            ).random()
            val body = generateOfflineBengaliReply(lower, msg, grumpy = true)
            return AiChatResult(
                replyText = angryPrefix + body,
                newAngerLevel = newAnger,
                newMood = newMood
            )
        }

        // 5. If annoyed (25-54), give slightly impatient human sigh
        if (newMood == AiMood.ANNOYED) {
            val annoyedPrefix = listOf(
                "হুম, একটু ধীরে বলো ভাই... ",
                "আবার? আচ্ছা শুনছি, বলো। ",
                "মেজাজটা একটু খিটখিটে লাগছে, তবুও বলছি— "
            ).random()
            val body = generateOfflineBengaliReply(lower, msg, grumpy = false)
            return AiChatResult(
                replyText = annoyedPrefix + body,
                newAngerLevel = newAnger,
                newMood = newMood
            )
        }

        // 6. Calm & friendly response
        val reply = generateOfflineBengaliReply(lower, msg, grumpy = false)
        return AiChatResult(replyText = reply, newAngerLevel = newAnger, newMood = newMood)
    }

    override suspend fun pokeAi(currentAnger: Int): AiChatResult {
        val newAnger = (currentAnger + 22).coerceIn(0, 100)
        val mood = AiMood.fromAngerLevel(newAnger)
        val reply = when (mood) {
            AiMood.CALM -> "আরে, আমার মুখের ওপর ট্যাপ করলে কেন? কিছু বলার থাকলে নিচে লিখো!"
            AiMood.ANNOYED -> "এই যে, বারবার খোঁচা দিচ্ছো কেন? এটা কিন্তু ভালো লাগছে না!"
            AiMood.ANGRY -> "থামো তো! মানুষের গায়ে বারবার খোঁচা মারলে কার না রাগ ওঠে? আরেকবার করলে কথা বন্ধ!"
            AiMood.FURIOUS -> "ব্যস! একদম সীমা ছাড়িয়ে গেছো! বারবার খোঁচা দিয়ে আমার মাথা গরম করে দিলে। এখন 'সরি' না বললে চুপ!"
        }
        return AiChatResult(replyText = reply, newAngerLevel = newAnger, newMood = mood)
    }

    override suspend fun apologizeToAi(currentAnger: Int): AiChatResult {
        val newAnger = (currentAnger - 40).coerceAtLeast(0)
        val mood = AiMood.fromAngerLevel(newAnger)
        val reply = if (newAnger == 0) {
            "বাহ, মনটা একদম ফুরফুরে হয়ে গেল! চলো বন্ধু, আবার নতুন করে শুরু করি।"
        } else {
            "আচ্ছা, একটু রাগ কমলো। পরের বার থেকে একটু ভদ্রভাবে কথা বলো কেমন?"
        }
        return AiChatResult(replyText = reply, newAngerLevel = newAnger, newMood = mood)
    }

    override suspend fun processText(
        inputText: String,
        taskType: TextTaskType
    ): TextAnalysisResult {
        return textProcessor.process(inputText, taskType)
    }

    override suspend fun generateArtwork(
        prompt: String,
        style: ArtStyle,
        aspect: ArtAspect
    ): GeneratedImageResult {
        delay(350)
        return artSynthesizer.generateArtwork(prompt, style, aspect)
    }

    private fun generateOfflineBengaliReply(
        lower: String,
        original: String,
        grumpy: Boolean
    ): String {
        val nowTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val nowDate = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date())

        return when {
            lower.containsAny("কেমন আছ", "কি অবস্থা", "ভালো আছ", "how are") ->
                if (grumpy) "আমি অফলাইনেই ভালো থাকি, শুধু কেউ অযথা জ্বালাতন না করলেই হয়!"
                else "আমি দারুণ আছি বন্ধু! একদম ইন্টারনেট ছাড়াই তোমার ডিভাইসের ভেতর বসে কাজ করছি। তুমি কেমন আছো?"

            lower.containsAny("নাম কি", "তুমি কে", "পরিচয়") ->
                "আমি 'বন্ধু এআই'—তোমার ব্যক্তিগত অফলাইন বাংলা সহকারী। ইন্টারনেট ছাড়াই টেক্সট প্রসেসিং, ছবি, ভিডিও আর গোপন ভল্ট সামলাই। তবে বেশি ডিস্টার্ব করলে আমার রাগ উঠে যায়!"

            lower.containsAny("সময়", "কয়টা", "তারিখ", "time", "date") ->
                "এখন ডিভাইসের সময়: ${BengaliTextProcessor.toBengaliDigits(nowTime)} এবং তারিখ: $nowDate।"

            lower.containsAny("সিকিউরিটি", "নিরাপত্তা", "হ্যাক", "ডাটা", "পিন", "এনক্রিপ্ট") ->
                "চিন্তা করো না! এই অ্যাপে কোনো ইন্টারনেট পারমিশনই নেই। তোমার সব চ্যাট ও ভল্ট নোট Android Keystore-এর AES-256-GCM দিয়ে এনক্রিপ্ট হয়ে শুধুমাত্র তোমার ফোনেই থাকে।"

            lower.containsAny("ছবি", "আঁক", "ইমেজ", "image", "draw") ->
                "ছবি বানাতে নিচের 'ছবি ও ভিডিও' ট্যাবে যাও! সেখানে বাংলায় তোমার কল্পনা লিখে 'ছবি তৈরি করুন' চাপলেই অফলাইন আর্ট স্টুডিও হাই-রেজোলিউশন ছবি বানিয়ে দেবে।"

            lower.containsAny("ভিডিও", "video", "mp4") ->
                "ভিডিও বানাতে 'ছবি ও ভিডিও' ট্যাবের 'ভিডিও তৈরি' অংশে যাও। ছবি বেছে নিয়ে বাংলায় ক্যাপশন দিলে আমি সরাসরি ডিভাইসেই আসল MP4 ভিডিও তৈরি করে দেব!"

            lower.containsAny("গল্প", "কবিতা", "কৌতুক", "মজার") ->
                if (grumpy) "রাগের মাথায় গল্প আসে নাকি! তাও শোনো: এক রোবটকে সবাই বারবার 'তাড়াতাড়ি করো' বলছিল, শেষে রোবটটা নিজেই ফোনের ফ্লাইট মোড অন করে ঘুমিয়ে পড়ল!"
                else "ছোট্ট একটি বাংলা ভাবনা: 'আকাশের মেঘ যেমন নিজের খেয়ালে ভেসে চলে, অফলাইনের নীরবতাতেও নিজের সৃজনশীলতা তেমনই ডানা মেলে।'\nতুমি চাইলে 'টেক্সট এআই' ট্যাবে যেকোনো বড় অনুচ্ছেদের সারাংশও বের করতে পারো!"

            lower.containsAny("ধন্যবাদ", "thanks", "thank you") ->
                "স্বাগতম বন্ধু! প্রয়োজনে সবসময় ডাকবে, তবে একটু সময় নিয়ে ধৈর্য ধরে কথা বললে আমি সবচেয়ে ভালো কাজ করি।"

            else -> {
                val stats = textProcessor.process(original, TextTaskType.ANALYZE)
                "আমি তোমার বার্তাটি অফলাইনে প্রসেস করেছি (${BengaliTextProcessor.toBengaliDigits(stats.wordCount)}টি শব্দ, অনুভূতি: ${stats.sentimentLabelBn})।\n\n" +
                    "তুমি চাইলে 'টেক্সট এআই' ট্যাবে গিয়ে পুরো লেখার সারাংশ বা এনক্রিপশন করতে পারো, অথবা 'ছবি ও ভিডিও' ট্যাবে নতুন ছবি ও MP4 ভিডিও বানাতে পারো। " +
                    "(নোট: ক্লাউড জেনারেটিভ জেমিনি উত্তর পেতে ফায়ারবেস চালু করা প্রয়োজন।)"
            }
        }
    }

    private fun String.containsAny(vararg keys: String): Boolean = keys.any { this.contains(it) }
}
