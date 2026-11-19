package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.BondhuUiState

@Composable
fun SecurityVaultScreen(
    uiState: BondhuUiState,
    onSetupPin: (String) -> Unit,
    onDisablePin: () -> Unit,
    onLockNow: () -> Unit,
    onToggleScreenPrivacy: (Boolean) -> Unit,
    onDeleteVaultNote: (Long) -> Unit,
    onWipeAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showPinDialog by remember { mutableStateOf(false) }
    var showWipeConfirm by remember { mutableStateOf(false) }

    if (showPinDialog) {
        SetupPinDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                onSetupPin(pin)
                showPinDialog = false
            }
        )
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("সব ডাটা মুছে ফেলবেন?") },
            text = {
                Text("এতে আপনার সব চ্যাট, ভল্ট নোট, ছবি, ভিডিও এবং পিন লক স্থায়ীভাবে মুছে যাবে।")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onWipeAllData()
                        showWipeConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("হ্যাঁ, সব মুছুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.security_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "আপনার কোনো তথ্য কখনোই ইন্টারনেটে পাঠানো হয় না",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Security Status Checklist Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "সক্রিয় নিরাপত্তা ঢাল",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    SecurityCheckRow(
                        label = "ইন্টারনেট অনুমতি সম্পূর্ণ বন্ধ (১০০% অফলাইন)"
                    )
                    SecurityCheckRow(
                        label = if (uiState.isHardwareKeystore) {
                            "হার্ডওয়্যার AndroidKeyStore AES-256-GCM এনক্রিপশন চালু"
                        } else {
                            "স্থানীয় AES-256-GCM এনক্রিপশন চালু"
                        }
                    )
                    SecurityCheckRow(
                        label = "ক্লাউড ব্যাকআপ নিষ্ক্রিয় (allowBackup = false)"
                    )
                }
            }

            // PIN Lock & Screen Privacy Controls
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.security_pin_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.securitySettings.isPinEnabled) {
                                    "৪-সংখ্যার পিন লক সক্রিয় আছে"
                                } else {
                                    "এখনও পিন সেট করা হয়নি"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showPinDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("setup_pin_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (uiState.securitySettings.isPinEnabled) "পিন বদলান" else "পিন সেট করুন")
                        }
                        if (uiState.securitySettings.isPinEnabled) {
                            OutlinedButton(
                                onClick = onLockNow,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("lock_app_now_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(stringResource(R.string.security_lock_now))
                            }
                        }
                    }

                    if (uiState.securitySettings.isPinEnabled) {
                        TextButton(
                            onClick = onDisablePin,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("পিন লক বন্ধ করুন", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhonelinkLock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.security_screen_privacy_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "চালু থাকলে স্ক্রিনশট, স্ক্রিন রেকর্ড ও রিমোট প্রিভিউতে স্ক্রিন কালো দেখাবে",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = uiState.securitySettings.isScreenPrivacyEnabled,
                            onCheckedChange = onToggleScreenPrivacy,
                            modifier = Modifier.testTag("screen_privacy_switch")
                        )
                    }
                }
            }

            // Encrypted Notes Vault
            Text(
                text = "এনক্রিপ্টেড গোপন ভল্ট (${uiState.vaultNotes.size}টি নোট)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (uiState.vaultNotes.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "টেক্সট এআই ট্যাব থেকে কোনো লেখা প্রসেস করে 'ভল্টে সেভ করুন' চাপলে এখানে AES-256 এনক্রিপ্টেড অবস্থায় জমা হবে।",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                uiState.vaultNotes.forEach { note ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onDeleteVaultNote(note.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "নোট মুছুন",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Text(
                                text = note.processedText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "এনক্রিপ্টেড কোড: ${note.encryptedPreview}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Emergency Wipe Button
            OutlinedButton(
                onClick = { showWipeConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("wipe_all_data_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Filled.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.security_wipe_all))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SecurityCheckRow(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SetupPinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("৪-সংখ্যার পিন সেট করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("অ্যাপ ও গোপন ভল্ট লক রাখতে ৪টি ইংরেজি/সংখ্যার ডিজিট দিন:")
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        if (value.length <= 4 && value.all { it.isDigit() }) {
                            pin = value
                        }
                    },
                    label = { Text("৪-সংখ্যার পিন") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.testTag("pin_setup_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pin) },
                enabled = pin.length == 4,
                modifier = Modifier.testTag("confirm_pin_setup_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
