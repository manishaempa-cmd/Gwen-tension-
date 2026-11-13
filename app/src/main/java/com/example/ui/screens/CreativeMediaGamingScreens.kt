package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.DeviceTelemetry
import com.example.core.GwenSettings
import com.example.data.local.GeneratedMediaEntity
import com.example.ui.components.GwenBottomNavBar
import com.example.ui.components.GwenCosmicBackground
import com.example.ui.components.GwenGlassCard
import com.example.ui.components.GwenScreenTopBar
import com.example.ui.components.GwenStatusPill
import com.example.ui.theme.GwenCyan
import com.example.ui.theme.GwenDanger
import com.example.ui.theme.GwenDeepBlack
import com.example.ui.theme.GwenElectricBlue
import com.example.ui.theme.GwenGlassBorder
import com.example.ui.theme.GwenSuccess
import com.example.ui.theme.GwenSurface
import com.example.ui.theme.GwenSurfaceElevated
import com.example.ui.theme.GwenTextMuted
import com.example.ui.theme.GwenTextPrimary
import com.example.ui.theme.GwenTextSecondary
import com.example.ui.theme.GwenViolet
import com.example.ui.theme.GwenWarning
import com.example.ui.viewmodel.ImageStudioUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageVideoStudioScreen(
    settings: GwenSettings,
    isEffectiveOnline: Boolean,
    imageState: ImageStudioUiState,
    galleryMedia: List<GeneratedMediaEntity>,
    onGenerateImage: (String, Bitmap?) -> Unit,
    onDeleteMedia: (GeneratedMediaEntity) -> Unit,
    onAskCreativeAi: (String) -> Unit,
    onBack: () -> Unit,
    onBottomNav: (String) -> Unit
) {
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("Futuristic cyberpunk holographic command desk in neon cyan and violet lighting") }
    var referenceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var videoPrompt by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            referenceBitmap = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    val promptPresets = listOf(
        "Sci-fi orbital command bridge",
        "Auburn-haired hero neon portrait",
        "Holographic galaxy HUD emblem",
        "Convert lighting to neon cyber dusk"
    )

    Scaffold(
        containerColor = GwenDeepBlack,
        bottomBar = { GwenBottomNavBar(currentKey = "studio", onSelect = onBottomNav) }
    ) { innerPadding ->
        GwenCosmicBackground(
            lightweightMode = settings.lightweightMode,
            modifier = Modifier.padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                GwenScreenTopBar(
                    title = "Image & Creative Studio",
                    subtitle = "Gemini Nano Banana 2.1 • Gallery",
                    onBack = onBack,
                    trailing = {
                        GwenStatusPill(
                            label = if (isEffectiveOnline) "CLOUD AI" else "OFFLINE",
                            color = if (isEffectiveOnline) GwenCyan else GwenWarning
                        )
                    }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GwenGlassCard(accentColor = GwenViolet) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GwenCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI Image Generation & Editing", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = prompt,
                            onValueChange = { prompt = it },
                            label = { Text("Creative Prompt Editor") },
                            minLines = 2,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GwenCyan,
                                unfocusedBorderColor = GwenGlassBorder,
                                focusedTextColor = GwenTextPrimary,
                                unfocusedTextColor = GwenTextPrimary,
                                focusedContainerColor = GwenSurface,
                                unfocusedContainerColor = GwenSurface
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("studio_prompt_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            promptPresets.forEach { preset ->
                                FilterChip(
                                    selected = false,
                                    onClick = { prompt = preset },
                                    label = { Text(preset, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }

                        if (referenceBitmap != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    bitmap = referenceBitmap!!.asImageBitmap(),
                                    contentDescription = "Edit reference image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Reference image attached for AI editing",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GwenCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { referenceBitmap = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear reference image", tint = GwenDanger)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                border = BorderStroke(1.dp, GwenViolet),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("studio_pick_edit_image_button")
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = GwenViolet)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Photo", color = GwenTextPrimary)
                            }
                            Button(
                                onClick = { onGenerateImage(prompt, referenceBitmap) },
                                enabled = imageState !is ImageStudioUiState.Generating && prompt.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = GwenCyan, contentColor = GwenDeepBlack),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("studio_generate_button")
                            ) {
                                Text(if (referenceBitmap != null) "Apply Edit" else "Generate")
                            }
                        }
                    }

                    when (imageState) {
                        is ImageStudioUiState.Generating -> {
                            GwenGlassCard(accentColor = GwenCyan) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(color = GwenCyan, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Synthesizing artwork via Gemini Nano Banana 2.1...", color = GwenTextPrimary)
                                }
                            }
                        }
                        is ImageStudioUiState.Error -> {
                            GwenGlassCard(accentColor = GwenDanger) {
                                Text(imageState.message, style = MaterialTheme.typography.bodySmall, color = GwenDanger)
                            }
                        }
                        is ImageStudioUiState.Success -> {
                            GwenGlassCard(accentColor = GwenSuccess) {
                                Text("Latest Generation", style = MaterialTheme.typography.titleSmall, color = GwenSuccess)
                                Spacer(modifier = Modifier.height(8.dp))
                                Image(
                                    bitmap = imageState.bitmap.asImageBitmap(),
                                    contentDescription = "Generated artwork",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                            }
                        }
                        is ImageStudioUiState.Idle -> Unit
                    }

                    // Video Integration & Script Assistant
                    GwenGlassCard(accentColor = GwenElectricBlue) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = GwenElectricBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Video Storyboard & Veo Integration", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Direct client-side video rendering is unavailable without a server-side Veo pipeline. Use Gemini below to script shot-by-shot video prompts & storyboards.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GwenTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = videoPrompt,
                            onValueChange = { videoPrompt = it },
                            placeholder = { Text("Describe scene for video script...", color = GwenTextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GwenElectricBlue,
                                unfocusedBorderColor = GwenGlassBorder,
                                focusedTextColor = GwenTextPrimary,
                                unfocusedTextColor = GwenTextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val topic = videoPrompt.ifBlank { prompt }
                                onAskCreativeAi("Create a 15-second cinematic video storyboard and Veo camera prompt for: $topic")
                            },
                            border = BorderStroke(1.dp, GwenElectricBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("studio_video_storyboard_button")
                        ) {
                            Text("Generate Video Storyboard with Gemini", color = GwenElectricBlue)
                        }
                    }

                    // Persisted Local Gallery
                    GwenGlassCard(accentColor = GwenCyan) {
                        Text(
                            "Generated Content Gallery (${galleryMedia.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = GwenCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (galleryMedia.isEmpty()) {
                            Text(
                                "Generated and edited images are automatically saved here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GwenTextSecondary
                            )
                        } else {
                            galleryMedia.take(10).forEach { media ->
                                val decoded by produceState<Bitmap?>(initialValue = null, key1 = media.filePath) {
                                    value = withContext(Dispatchers.IO) {
                                        try {
                                            BitmapFactory.decodeFile(media.filePath)
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (decoded != null) {
                                        Image(
                                            bitmap = decoded!!.asImageBitmap(),
                                            contentDescription = media.prompt,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(GwenSurfaceElevated),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null, tint = GwenTextMuted)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = media.prompt,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GwenTextPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (media.isEdit) "${media.modelName} • Edit" else media.modelName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GwenCyan
                                        )
                                    }
                                    IconButton(onClick = { onDeleteMedia(media) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete artwork", tint = GwenDanger)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GamingAssistantScreen(
    settings: GwenSettings,
    telemetry: DeviceTelemetry,
    isEffectiveOnline: Boolean,
    onSaveHudNotes: (String) -> Unit,
    onRefreshTelemetry: () -> Unit,
    onAskGamingAi: (String) -> Unit,
    onToggleLightweight: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var hudNotes by remember(settings.hudSensitivityNotes) {
        mutableStateOf(settings.hudSensitivityNotes)
    }
    var gameQuery by remember { mutableStateOf("") }

    val voicePresets = listOf(
        "Best 4-finger claw sensitivity for BGMI / CODM",
        "Counter-strategy against aggressive rushers",
        "Optimize Galaxy A17 5G for stable 60fps",
        "Top ranked loadout tips right now"
    )

    GwenCosmicBackground(lightweightMode = settings.lightweightMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            GwenScreenTopBar(
                title = "Gaming Assistant",
                subtitle = "Telemetry • HUD Notes • Tactical AI",
                onBack = onBack,
                trailing = {
                    IconButton(onClick = onRefreshTelemetry) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh telemetry", tint = GwenCyan)
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GwenGlassCard(accentColor = GwenCyan) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SportsEsports, contentDescription = null, tint = GwenCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(telemetry.deviceModel, style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val ramRatio = if (telemetry.ramTotalMb > 0) {
                        (telemetry.ramUsedMb.toFloat() / telemetry.ramTotalMb.toFloat()).coerceIn(0f, 1f)
                    } else 0.5f
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = GwenViolet, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "RAM: ${telemetry.ramAvailableMb} MB free / ${telemetry.ramTotalMb} MB total",
                            style = MaterialTheme.typography.bodySmall,
                            color = GwenTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { ramRatio },
                        color = if (ramRatio > 0.82f) GwenWarning else GwenCyan,
                        trackColor = GwenSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = GwenSuccess, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${telemetry.batteryPercent}% ${if (telemetry.isCharging) "(Charging)" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GwenTextSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Thermostat, contentDescription = null, tint = GwenWarning, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "${telemetry.batteryTempC}°C Thermal",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (telemetry.batteryTempC > 41f) GwenDanger else GwenTextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onToggleLightweight(!settings.lightweightMode) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (settings.lightweightMode) "Lightweight Overlay: ON (Low RAM)" else "Enable Low-RAM Gaming Overlay Mode",
                            color = GwenCyan
                        )
                    }
                }

                GwenGlassCard(accentColor = GwenViolet) {
                    Text("Sensitivity & HUD Notes", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = hudNotes,
                        onValueChange = { hudNotes = it },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GwenViolet,
                            unfocusedBorderColor = GwenGlassBorder,
                            focusedTextColor = GwenTextPrimary,
                            unfocusedTextColor = GwenTextPrimary,
                            focusedContainerColor = GwenSurface,
                            unfocusedContainerColor = GwenSurface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gaming_hud_notes_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onSaveHudNotes(hudNotes) },
                        colors = ButtonDefaults.buttonColors(containerColor = GwenViolet),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("gaming_save_hud_button")
                    ) {
                        Text("Save HUD & Sync to Memory")
                    }
                }

                GwenGlassCard(accentColor = GwenElectricBlue) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tactical Strategy Coach", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                        GwenStatusPill(
                            label = if (isEffectiveOnline) "GEMINI GAMING" else "OFFLINE",
                            color = if (isEffectiveOnline) GwenSuccess else GwenWarning
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = gameQuery,
                        onValueChange = { gameQuery = it },
                        placeholder = { Text("Ask game tips, weapon meta, or strategy...", color = GwenTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GwenCyan,
                            unfocusedBorderColor = GwenGlassBorder,
                            focusedTextColor = GwenTextPrimary,
                            unfocusedTextColor = GwenTextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (gameQuery.isNotBlank()) {
                                onAskGamingAi(gameQuery)
                                gameQuery = ""
                            }
                        },
                        enabled = gameQuery.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = GwenCyan, contentColor = GwenDeepBlack),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("gaming_ask_button")
                    ) {
                        Text("Ask Gwen Gaming AI")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        voicePresets.forEach { preset ->
                            FilterChip(
                                selected = false,
                                onClick = { onAskGamingAi(preset) },
                                label = { Text(preset, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MusicCenterScreen(
    settings: GwenSettings,
    telemetry: DeviceTelemetry,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSetVolume: (Int) -> Unit,
    onOpenYouTubeSearch: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    var youtubeQuery by remember { mutableStateOf("") }

    GwenCosmicBackground(lightweightMode = settings.lightweightMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            GwenScreenTopBar(
                title = "Music & Media Center",
                subtitle = "Hardware Media Controls & YouTube",
                onBack = onBack,
                trailing = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh media state", tint = GwenCyan)
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GwenGlassCard(accentColor = GwenCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = GwenCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Media Playback", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                        }
                        GwenStatusPill(
                            label = if (telemetry.isMusicActive) "PLAYING" else "IDLE",
                            color = if (telemetry.isMusicActive) GwenSuccess else GwenTextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPrevious,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(GwenSurfaceElevated)
                                .testTag("music_prev_button")
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous track", tint = GwenCyan, modifier = Modifier.size(30.dp))
                        }

                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(GwenCyan)
                                .testTag("music_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (telemetry.isMusicActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play or pause music",
                                tint = GwenDeepBlack,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = onNext,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(GwenSurfaceElevated)
                                .testTag("music_next_button")
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next track", tint = GwenCyan, modifier = Modifier.size(30.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = GwenViolet)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Media Volume: ${telemetry.mediaVolume} / ${telemetry.mediaMaxVolume}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GwenTextSecondary
                        )
                    }
                    Slider(
                        value = telemetry.mediaVolume.toFloat(),
                        onValueChange = { onSetVolume(it.toInt()) },
                        valueRange = 0f..telemetry.mediaMaxVolume.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = GwenCyan, activeTrackColor = GwenCyan)
                    )
                }

                GwenGlassCard(accentColor = GwenViolet) {
                    Text("YouTube Supported Navigation", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Launch YouTube directly or jump straight to music, tutorials, or synthwave playlists.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GwenTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = youtubeQuery,
                        onValueChange = { youtubeQuery = it },
                        placeholder = { Text("Search YouTube...", color = GwenTextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GwenViolet,
                            unfocusedBorderColor = GwenGlassBorder,
                            focusedTextColor = GwenTextPrimary,
                            unfocusedTextColor = GwenTextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("music_youtube_query_input")
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onOpenYouTubeSearch(youtubeQuery) },
                            colors = ButtonDefaults.buttonColors(containerColor = GwenDanger, contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("music_open_youtube_button")
                        ) {
                            Text(if (youtubeQuery.isBlank()) "Open YouTube" else "Search YouTube")
                        }
                        OutlinedButton(
                            onClick = { onOpenYouTubeSearch("lofi cyberpunk chill beats") },
                            border = BorderStroke(1.dp, GwenCyan),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Focus Beats", color = GwenCyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
