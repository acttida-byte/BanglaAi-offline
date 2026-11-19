package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.AiMood
import com.example.domain.ChatMessage
import com.example.ui.BondhuUiState
import com.example.ui.components.AngerMeterBar
import com.example.ui.components.FirebaseNoticeBanner
import com.example.ui.components.OfflineSecurityHeaderStrip
import com.example.ui.components.moodColorFor
import kotlin.math.roundToInt

@Composable
fun ChatScreen(
    uiState: BondhuUiState,
    onSendMessage: (String) -> Unit,
    onPokeAvatar: () -> Unit,
    onApologize: () -> Unit,
    onClearChat: () -> Unit,
    onLockApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val shakeOffset = remember { Animatable(0f) }

    val quickPrompts = listOf(
        "কেমন আছো বন্ধু?",
        "সময় কয়টা বাজে?",
        "সিকিউরিটি কেমন?",
        "একটা মজার গল্প বলো",
        "এই তাড়াতাড়ি উত্তর দাও!!",
        "সরি বন্ধু, রাগ করো না"
    )

    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatMessages.lastIndex)
        }
    }

    LaunchedEffect(uiState.angerLevel) {
        if (uiState.angerLevel >= 55) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            for (delta in listOf(-14f, 14f, -10f, 10f, -5f, 0f)) {
                shakeOffset.animateTo(delta, animationSpec = tween(45))
            }
        }
    }

    val moodColor = moodColorFor(uiState.mood)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Mood & Temperament Hero Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.2.dp, moodColor.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val avatarRes = if (uiState.mood == AiMood.ANGRY || uiState.mood == AiMood.FURIOUS) {
                            R.drawable.img_ai_angry
                        } else {
                            R.drawable.img_ai_calm
                        }
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .border(2.dp, moodColor, CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onPokeAvatar()
                                }
                                .testTag("poke_ai_avatar")
                        ) {
                            Image(
                                painter = painterResource(id = avatarRes),
                                contentDescription = stringResource(R.string.chat_avatar_desc),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.chat_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (uiState.mood == AiMood.ANGRY || uiState.mood == AiMood.FURIOUS) {
                                    Icon(
                                        imageVector = Icons.Filled.Whatshot,
                                        contentDescription = null,
                                        tint = moodColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = "মুখে ট্যাপ করে খোঁচা দিলে দ্রুত রাগ করবে!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OfflineSecurityHeaderStrip()
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            if (uiState.securitySettings.isPinEnabled) {
                                IconButton(
                                    onClick = onLockApp,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("quick_lock_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = stringResource(R.string.security_lock_now),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(
                                onClick = onClearChat,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("clear_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CleaningServices,
                                    contentDescription = stringResource(R.string.chat_clear_desc),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    AngerMeterBar(angerLevel = uiState.angerLevel, mood = uiState.mood)

                    AnimatedVisibility(visible = uiState.angerLevel >= 35) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onApologize,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("apologize_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.btn_apologize))
                            }
                            OutlinedButton(
                                onClick = { onSendMessage("শান্ত হও বন্ধু, দুঃখিত") },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("calm_down_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(stringResource(R.string.btn_calm_down))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            FirebaseNoticeBanner()
            Spacer(modifier = Modifier.height(8.dp))

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(uiState.chatMessages, key = { it.id }) { msg ->
                    ChatBubbleItem(message = msg)
                }
                if (uiState.isAiTyping) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (uiState.angerLevel >= 55) "রাগে ফুঁসছে..." else "অফলাইনে ভাবছে...",
                                style = MaterialTheme.typography.bodySmall,
                                color = moodColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Quick Bengali Prompt Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                items(quickPrompts) { prompt ->
                    AssistChip(
                        onClick = { onSendMessage(prompt) },
                        label = {
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        modifier = Modifier.testTag("quick_prompt_chip")
                    )
                }
            }

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    placeholder = {
                        Text(
                            text = if (uiState.mood == AiMood.FURIOUS) {
                                stringResource(R.string.chat_input_furious_hint)
                            } else {
                                stringResource(R.string.chat_input_hint)
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                    maxLines = 3
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("send_chat_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = moodColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.chat_send_desc)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderCol = if (message.isUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else {
        moodColorFor(message.mood).copy(alpha = 0.4f)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 310.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isUser) 18.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 18.dp
            ),
            color = bubbleColor,
            border = BorderStroke(1.dp, borderCol)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (!message.isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(moodColorFor(message.mood))
                        )
                        Text(
                            text = "বন্ধু এআই • ${message.mood.emojiTag}",
                            style = MaterialTheme.typography.labelSmall,
                            color = moodColorFor(message.mood),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
                if (message.isOfflineEngineNotice) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "অফলাইন লোকাল রেসপন্স • ক্লাউড এআই-এর জন্য ফায়ারবেস লাগবে",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
