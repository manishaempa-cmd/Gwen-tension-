package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.GwenSettings
import com.example.data.model.AvatarMode
import com.example.data.model.GwenExpression
import com.example.ui.components.GwenAvatarStage
import com.example.ui.components.GwenCosmicBackground
import com.example.ui.components.GwenGlassCard
import com.example.ui.components.GwenScreenTopBar
import com.example.ui.components.VoiceWaveformVisualizer
import com.example.ui.theme.GwenCyan
import com.example.ui.theme.GwenDanger
import com.example.ui.theme.GwenDeepBlack
import com.example.ui.theme.GwenElectricBlue
import com.example.ui.theme.GwenTextPrimary
import com.example.ui.theme.GwenTextSecondary
import com.example.ui.theme.GwenViolet

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvatarStudioScreen(
    settings: GwenSettings,
    expression: GwenExpression,
    isSpeaking: Boolean,
    rmsLevel: Float,
    onSelectAvatarMode: (AvatarMode) -> Unit,
    onSelectExpression: (GwenExpression) -> Unit,
    onPreviewSpeech: () -> Unit,
    onStopSpeech: () -> Unit,
    onToggleLightweight: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    GwenCosmicBackground(lightweightMode = settings.lightweightMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            GwenScreenTopBar(
                title = "Avatar Studio",
                subtitle = "2D Animated & 3D Hologram Modes",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GwenAvatarStage(
                    avatarMode = settings.avatarMode,
                    expression = expression,
                    isSpeaking = isSpeaking,
                    isListening = false,
                    rmsLevel = rmsLevel,
                    lightweightMode = settings.lightweightMode,
                    size = 225.dp
                )

                GwenGlassCard(accentColor = GwenCyan) {
                    Text("Avatar Projection Mode", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AvatarMode.entries.forEach { mode ->
                            FilterChip(
                                selected = mode == settings.avatarMode,
                                onClick = { onSelectAvatarMode(mode) },
                                label = { Text(mode.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GwenCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = GwenCyan
                                ),
                                modifier = Modifier.testTag("avatar_mode_${mode.name}")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = settings.avatarMode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = GwenTextSecondary
                    )
                }

                GwenGlassCard(accentColor = GwenViolet) {
                    Text("10 Expressive Mood States", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GwenExpression.entries.forEach { exp ->
                            FilterChip(
                                selected = exp == expression,
                                onClick = { onSelectExpression(exp) },
                                label = { Text(exp.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GwenViolet.copy(alpha = 0.26f),
                                    selectedLabelColor = GwenCyan
                                ),
                                modifier = Modifier.testTag("expression_${exp.name}")
                            )
                        }
                    }
                }

                GwenGlassCard(accentColor = GwenElectricBlue) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lightweight Mode (6GB RAM)", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                            Text("Reduces radial glow & scanline frame cost.", style = MaterialTheme.typography.bodySmall, color = GwenTextSecondary)
                        }
                        Switch(
                            checked = settings.lightweightMode,
                            onCheckedChange = onToggleLightweight,
                            colors = SwitchDefaults.colors(checkedThumbColor = GwenCyan)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onPreviewSpeech,
                            colors = ButtonDefaults.buttonColors(containerColor = GwenCyan, contentColor = GwenDeepBlack),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("avatar_test_lipsync_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Lip-Sync")
                        }
                        OutlinedButton(
                            onClick = onStopSpeech,
                            enabled = isSpeaking,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = GwenDanger)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop", color = GwenTextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun VoiceStudioScreen(
    settings: GwenSettings,
    isSpeaking: Boolean,
    isListening: Boolean,
    rmsLevel: Float,
    wakeServiceRunning: Boolean,
    onUpdateSettings: ((GwenSettings) -> GwenSettings) -> Unit,
    onPreviewVoice: () -> Unit,
    onStopSpeaking: () -> Unit,
    onTestWakeListening: () -> Unit,
    onToggleBackgroundService: (Boolean) -> Unit,
    onOpenRegistration: () -> Unit,
    onOpenVerification: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
    }

    GwenCosmicBackground(lightweightMode = settings.lightweightMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            GwenScreenTopBar(
                title = "Voice Studio",
                subtitle = "Wake \"Gwen\" • Hindi / English / Hinglish",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GwenGlassCard(accentColor = GwenCyan) {
                    Text("Voice Activity Monitor", style = MaterialTheme.typography.titleMedium, color = GwenCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    VoiceWaveformVisualizer(
                        rmsLevel = rmsLevel,
                        isActive = isListening,
                        isSpeaking = isSpeaking,
                        lightweightMode = settings.lightweightMode
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onPreviewVoice,
                            colors = ButtonDefaults.buttonColors(containerColor = GwenCyan, contentColor = GwenDeepBlack),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("voice_preview_button")
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview Voice")
                        }
                        OutlinedButton(
                            onClick = {
                                if (isSpeaking) onStopSpeaking()
                                else if (micGranted) onTestWakeListening()
                                else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("voice_test_wake_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = GwenViolet)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isSpeaking) "Stop TTS" else "Say \"Gwen\"", color = GwenTextPrimary)
                        }
                    }
                }

                GwenGlassCard(accentColor = GwenViolet) {
                    Text("Recognition Language", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("en-IN" to "English", "hi-IN" to "Hindi", "hinglish" to "Hinglish").forEach { (code, label) ->
                            FilterChip(
                                selected = settings.preferredLanguage == code,
                                onClick = { onUpdateSettings { it.copy(preferredLanguage = code) } },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GwenViolet.copy(alpha = 0.25f),
                                    selectedLabelColor = GwenCyan
                                )
                            )
                        }
                    }
                }

                GwenGlassCard(accentColor = GwenElectricBlue) {
                    Text("Female AI Voice Calibration", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Pitch: ${"%.2f".format(settings.voicePitch)}x", style = MaterialTheme.typography.bodySmall, color = GwenTextSecondary)
                    Slider(
                        value = settings.voicePitch,
                        onValueChange = { v -> onUpdateSettings { it.copy(voicePitch = v) } },
                        valueRange = 0.7f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = GwenCyan, activeTrackColor = GwenCyan)
                    )
                    Text("Speed: ${"%.2f".format(settings.voiceSpeed)}x", style = MaterialTheme.typography.bodySmall, color = GwenTextSecondary)
                    Slider(
                        value = settings.voiceSpeed,
                        onValueChange = { v -> onUpdateSettings { it.copy(voiceSpeed = v) } },
                        valueRange = 0.7f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = GwenViolet, activeTrackColor = GwenViolet)
                    )
                    Text("Volume: ${(settings.voiceVolume * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = GwenTextSecondary)
                    Slider(
                        value = settings.voiceVolume,
                        onValueChange = { v -> onUpdateSettings { it.copy(voiceVolume = v) } },
                        valueRange = 0.2f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = GwenElectricBlue, activeTrackColor = GwenElectricBlue)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Speak AI Replies", style = MaterialTheme.typography.bodyMedium, color = GwenTextPrimary)
                        Switch(
                            checked = settings.autoSpeakResponses,
                            onCheckedChange = { v -> onUpdateSettings { it.copy(autoSpeakResponses = v) } }
                        )
                    }
                }

                GwenGlassCard(accentColor = GwenCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Background \"Gwen\" Service", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                            Text(
                                if (wakeServiceRunning) "Foreground microphone channel active" else "Low-battery idle state",
                                style = MaterialTheme.typography.bodySmall,
                                color = GwenTextSecondary
                            )
                        }
                        Switch(
                            checked = wakeServiceRunning,
                            onCheckedChange = { enabled ->
                                if (enabled && !micGranted) {
                                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    onToggleBackgroundService(enabled)
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Wake Sensitivity: ${(settings.wakeWordSensitivity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = GwenTextSecondary
                    )
                    Slider(
                        value = settings.wakeWordSensitivity,
                        onValueChange = { v -> onUpdateSettings { it.copy(wakeWordSensitivity = v) } },
                        valueRange = 0.3f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = GwenCyan, activeTrackColor = GwenCyan)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onOpenRegistration,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("voice_studio_enroll_btn")
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = GwenCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enroll Voice", color = GwenCyan)
                    }
                    OutlinedButton(
                        onClick = onOpenVerification,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("voice_studio_verify_btn")
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GwenViolet)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify Voice", color = GwenViolet)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
