package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.BengaliTextProcessor
import com.example.domain.AiMood
import com.example.ui.theme.MoodAngryColor
import com.example.ui.theme.MoodAnnoyedColor
import com.example.ui.theme.MoodCalmColor
import com.example.ui.theme.MoodFuriousColor

@Composable
fun moodColorFor(mood: AiMood): Color {
    val target = when (mood) {
        AiMood.CALM -> MoodCalmColor
        AiMood.ANNOYED -> MoodAnnoyedColor
        AiMood.ANGRY -> MoodAngryColor
        AiMood.FURIOUS -> MoodFuriousColor
    }
    val animated by animateColorAsState(targetValue = target, label = "mood_color")
    return animated
}

@Composable
fun StatusPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun OfflineSecurityHeaderStrip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusPill(
            icon = Icons.Filled.CloudOff,
            label = stringResource(R.string.status_offline_active),
            tint = MaterialTheme.colorScheme.primary
        )
        StatusPill(
            icon = Icons.Filled.Lock,
            label = stringResource(R.string.status_encrypted_local),
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun FirebaseNoticeBanner(
    text: String = stringResource(R.string.notice_firebase_required),
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AngerMeterBar(
    angerLevel: Int,
    mood: AiMood,
    modifier: Modifier = Modifier
) {
    val moodColor = moodColorFor(mood)
    val animatedProgress by animateFloatAsState(
        targetValue = (angerLevel / 100f).coerceIn(0f, 1f),
        animationSpec = spring(),
        label = "anger_progress"
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stringResource(R.string.mood_meter_title)}: ${mood.labelBn}",
                style = MaterialTheme.typography.labelMedium,
                color = moodColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${BengaliTextProcessor.toBengaliDigits(angerLevel)}%",
                style = MaterialTheme.typography.labelMedium,
                color = moodColor,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = moodColor,
            trackColor = moodColor.copy(alpha = 0.18f)
        )
    }
}
