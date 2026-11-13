package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.core.GwenSettings
import com.example.data.model.AiChatMode
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.GwenExpression
import com.example.ui.components.GwenAvatarStage
import com.example.ui.components.GwenBottomNavBar
import com.example.ui.components.GwenCosmicBackground
import com.example.ui.components.GwenGlassCard
import com.example.ui.components.GwenScreenTopBar
import com.example.ui.components.GwenStatusPill
import com.example.ui.components.VoiceWaveformVisualizer
import com.example.ui.navigation.GwenRoute
import com.example.ui.theme.GwenCyan
import com.example.ui.theme.GwenDanger
import com.example.ui.theme.GwenDeepBlack
import com.example.ui.theme.GwenElectricBlue
import com.example.ui.theme.GwenGlassBorder
import com.example.ui.theme.GwenObsidian
import com.example.ui.theme.GwenSuccess
import com.example.ui.theme.GwenSurface
import com.example.ui.theme.GwenSurfaceElevated
import com.example.ui.theme.GwenTextMuted
import com.example.ui.theme.GwenTextPrimary
import com.example.ui.theme.GwenTextSecondary
import com.example.ui.theme.GwenViolet
import com.example.ui.theme.GwenWarning
import com.example.ui.viewmodel.AiStreamState
import com.example.ui.viewmodel.DataUiState

private data class HubTile(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val route: GwenRoute,
    val tag: String
)

@Composable
fun MainDashboardScreen(
    settings: GwenSettings,
    expression: GwenExpression,
    liveStatus: String,
    isEffectiveOnline: Boolean,
    isSpeaking: Boolean,
    isListening: Boolean,
    rmsLevel: Float,
    partialTranscript: String,
    voiceError: String?,
    aiStreamState: AiStreamState,
    conversationsState: DataUiState<List<Conversation>>,
    messagesState: DataUiState<List<ChatMessage>>,
    activeConversationId: String,
    onSelectConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onStopSpeaking: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onNavigate: (GwenRoute) -> Unit,
    onBottomNav: (String) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
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
        if (granted) onStartVoice()
    }

    val hubTiles = remember {
        listOf(
            HubTile("Avatar Studio", "Modes & 10 moods", Icons.Default.Face, GwenCyan, GwenRoute.AvatarStudio, "tile_avatar"),
            HubTile("Voice Studio", "Wake & Hindi/Hinglish", Icons.Default.GraphicEq, GwenViolet, GwenRoute.VoiceStudio, "tile_voice"),
            HubTile("Phone Control", "Apps, calls & toggles", Icons.Default.Devices, GwenElectricBlue, GwenRoute.PhoneControl, "tile_phone"),
            HubTile("Screen Vision", "OCR & UI analyzer", Icons.Default.ScreenshotMonitor, GwenCyan, GwenRoute.ScreenUnderstanding, "tile_screen"),
            HubTile("Task Center", "Verified 7-step flow", Icons.Default.Checklist, GwenSuccess, GwenRoute.TaskManager, "tile_tasks"),
            HubTile("Creative Studio", "Image & video hub", Icons.Default.AutoAwesome, GwenViolet, GwenRoute.ImageVideoStudio, "tile_creative"),
            HubTile("Gaming Assist", "HUD & RAM boost", Icons.Default.SportsEsports, GwenElectricBlue, GwenRoute.GamingAssistant, "tile_gaming"),
            HubTile("Music Center", "Media & YouTube", Icons.Default.MusicNote, GwenCyan, GwenRoute.MusicCenter, "tile_music"),
            HubTile("Memory Core", "Notes & context", Icons.Default.Memory, GwenViolet, GwenRoute.MemoryCenter, "tile_memory"),
            HubTile("Online/Offline", "Hybrid AI matrix", Icons.Default.CloudDone, GwenElectricBlue, GwenRoute.OnlineOfflineMode, "tile_mode"),
            HubTile("Security Center", "Voice ID & PIN", Icons.Default.Security, GwenSuccess, GwenRoute.SecurityCenter, "tile_security"),
            HubTile("Permissions", "System access hub", Icons.Default.Shield, GwenWarning, GwenRoute.PermissionsCenter, "tile_perms"),
            HubTile("AI Diagnostics", "Gemini & App Check", Icons.Default.MonitorHeart, GwenCyan, GwenRoute.AiStatusDashboard, "tile_status")
        )
    }

    Scaffold(
        containerColor = GwenDeepBlack,
        bottomBar = { GwenBottomNavBar(currentKey = "dashboard", onSelect = onBottomNav) }
    ) { innerPadding ->
        GwenCosmicBackground(
            lightweightMode = settings.lightweightMode,
            modifier = Modifier.padding(innerPadding)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Header Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GwenStatusPill(
                            label = if (isEffectiveOnline) "ONLINE" else "OFFLINE CORE",
                            color = if (isEffectiveOnline) GwenSuccess else GwenWarning,
                            icon = if (isEffectiveOnline) Icons.Default.CloudDone else Icons.Default.CloudOff
                        )
                        GwenStatusPill(
                            label = if (isEffectiveOnline) settings.preferredModel.uppercase() else "LOCAL AI",
                            color = GwenCyan,
                            icon = Icons.Default.Psychology
                        )
                        GwenStatusPill(
                            label = if (settings.batteryFriendlyMode) "ECO BATTERY" else "MAX POWER",
                            color = GwenViolet,
                            icon = Icons.Default.BatterySaver
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Greeting & Live Status
                    Text(
                        text = stringResource(R.string.welcome_greeting),
                        style = MaterialTheme.typography.displayMedium,
                        color = GwenTextPrimary
                    )
                    Text(
                        text = liveStatus,
                        style = MaterialTheme.typography.labelMedium,
                        color = GwenCyan
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Central Animated Gwen Avatar
                    GwenAvatarStage(
                        avatarMode = settings.avatarMode,
                        expression = expression,
                        isSpeaking = isSpeaking,
                        isListening = isListening,
                        rmsLevel = rmsLevel,
                        lightweightMode = settings.lightweightMode,
                        size = 210.dp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Waveform & Thinking indicator
                    VoiceWaveformVisualizer(
                        rmsLevel = rmsLevel,
                        isActive = isListening,
                        isSpeaking = isSpeaking,
                        lightweightMode = settings.lightweightMode,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    if (partialTranscript.isNotBlank()) {
                        Text(
                            text = "\"$partialTranscript\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = GwenCyan,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (voiceError != null) {
                        Text(
                            text = voiceError,
                            style = MaterialTheme.typography.bodySmall,
                            color = GwenWarning,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Voice Controls Row: New Chat | Big Mic | Stop Speaking
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNewConversation,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(GwenSurfaceElevated)
                                .border(1.dp, GwenGlassBorder, CircleShape)
                                .testTag("dashboard_new_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddComment,
                                contentDescription = stringResource(R.string.cd_new_conversation),
                                tint = GwenCyan
                            )
                        }

                        // Large Central Microphone Button
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        if (isListening) listOf(GwenDanger, GwenViolet)
                                        else listOf(GwenCyan, GwenElectricBlue)
                                    )
                                )
                                .border(2.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                                .minimumInteractiveComponentSize()
                                .clickable {
                                    if (isListening) {
                                        onStopVoice()
                                    } else if (micGranted) {
                                        onStartVoice()
                                    } else {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                                .testTag("dashboard_mic_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = stringResource(R.string.cd_mic_button),
                                tint = GwenDeepBlack,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        IconButton(
                            onClick = onStopSpeaking,
                            enabled = isSpeaking,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSpeaking) GwenDanger.copy(alpha = 0.25f) else GwenSurfaceElevated
                                )
                                .border(
                                    1.dp,
                                    if (isSpeaking) GwenDanger else GwenGlassBorder,
                                    CircleShape
                                )
                                .testTag("dashboard_stop_speaking_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = stringResource(R.string.cd_stop_speaking),
                                tint = if (isSpeaking) GwenDanger else GwenTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text Input + Send Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask Gwen anything, Boss...", color = GwenTextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GwenCyan,
                                unfocusedBorderColor = GwenGlassBorder,
                                focusedTextColor = GwenTextPrimary,
                                unfocusedTextColor = GwenTextPrimary,
                                focusedContainerColor = GwenSurface,
                                unfocusedContainerColor = GwenSurface
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_prompt_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    onSendPrompt(inputText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(GwenCyan)
                                .testTag("dashboard_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.cd_send_message),
                                tint = GwenDeepBlack
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Response / Latest Conversation Preview Card
                    val latestReply = when (aiStreamState) {
                        is AiStreamState.Thinking -> "Gwen is analyzing your request..."
                        is AiStreamState.Streaming -> aiStreamState.partialText
                        is AiStreamState.Error -> "Error: ${aiStreamState.message}"
                        is AiStreamState.Idle -> {
                            val msgs = (messagesState as? DataUiState.Success)?.data.orEmpty()
                            msgs.lastOrNull()?.text ?: "Tap the mic or type a command to begin, Boss."
                        }
                    }

                    GwenGlassCard(
                        accentColor = if (aiStreamState is AiStreamState.Error) GwenDanger else GwenCyan,
                        onClick = { onNavigate(GwenRoute.AiChat) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_preview_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LATEST RESPONSE",
                                style = MaterialTheme.typography.labelMedium,
                                color = GwenCyan
                            )
                            if (aiStreamState is AiStreamState.Thinking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = GwenViolet,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Open Full Chat →",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GwenTextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = latestReply,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (aiStreamState is AiStreamState.Error) GwenDanger else GwenTextPrimary,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Conversation History Chips
                    val conversations = (conversationsState as? DataUiState.Success)?.data.orEmpty()
                    if (conversations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = GwenViolet,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recent Sessions",
                                style = MaterialTheme.typography.labelMedium,
                                color = GwenTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(conversations.take(8), key = { it.id }) { conv ->
                                val selected = conv.id == activeConversationId
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onSelectConversation(conv.id)
                                        onNavigate(GwenRoute.AiChat)
                                    },
                                    label = {
                                        Text(
                                            text = conv.title.take(22),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GwenViolet.copy(alpha = 0.25f),
                                        selectedLabelColor = GwenCyan
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gwen Command Hub (All Modules)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GWEN COMMAND MODULES",
                            style = MaterialTheme.typography.labelLarge,
                            color = GwenCyan
                        )
                        Text(
                            text = "${hubTiles.size} Systems",
                            style = MaterialTheme.typography.labelSmall,
                            color = GwenTextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    hubTiles.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { tile ->
                                GwenGlassCard(
                                    accentColor = tile.color,
                                    onClick = { onNavigate(tile.route) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(tile.tag)
                                ) {
                                    Icon(
                                        imageVector = tile.icon,
                                        contentDescription = tile.title,
                                        tint = tile.color,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = tile.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = GwenTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tile.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GwenTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    settings: GwenSettings,
    chatMode: AiChatMode,
    isEffectiveOnline: Boolean,
    isSpeaking: Boolean,
    isListening: Boolean,
    aiStreamState: AiStreamState,
    conversationsState: DataUiState<List<Conversation>>,
    messagesState: DataUiState<List<ChatMessage>>,
    activeConversationId: String,
    onSelectMode: (AiChatMode) -> Unit,
    onSelectConversation: (String) -> Unit,
    onNewConversation: () -> Unit,
    onStartVoice: () -> Unit,
    onStopSpeaking: () -> Unit,
    onSendPrompt: (String, Bitmap?) -> Unit,
    onBack: () -> Unit,
    onBottomNav: (String) -> Unit
) {
    val context = LocalContext.current
    var promptText by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showHistorySheet by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachedBitmap = try {
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

    val messages = (messagesState as? DataUiState.Success)?.data.orEmpty()
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, aiStreamState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0))
        }
    }

    Scaffold(
        containerColor = GwenDeepBlack,
        bottomBar = { GwenBottomNavBar(currentKey = "chat", onSelect = onBottomNav) }
    ) { innerPadding ->
        GwenCosmicBackground(
            lightweightMode = settings.lightweightMode,
            modifier = Modifier.padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                GwenScreenTopBar(
                    title = "Gemini AI Brain",
                    subtitle = if (isEffectiveOnline) "${settings.preferredModel} • ${chatMode.label}" else "Offline Core Mode",
                    onBack = onBack,
                    trailing = {
                        Row {
                            IconButton(
                                onClick = { showHistorySheet = true },
                                modifier = Modifier.testTag("chat_history_button")
                            ) {
                                Icon(Icons.Default.History, contentDescription = "History", tint = GwenCyan)
                            }
                            IconButton(
                                onClick = onNewConversation,
                                modifier = Modifier.testTag("chat_new_button")
                            ) {
                                Icon(Icons.Default.AddComment, contentDescription = stringResource(R.string.cd_new_conversation), tint = GwenViolet)
                            }
                        }
                    }
                )

                // Mode Selector Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiChatMode.entries.forEach { mode ->
                        FilterChip(
                            selected = mode == chatMode,
                            onClick = { onSelectMode(mode) },
                            label = { Text(mode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GwenCyan.copy(alpha = 0.22f),
                                selectedLabelColor = GwenCyan
                            )
                        )
                    }
                }

                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (messages.isEmpty() && aiStreamState is AiStreamState.Idle) {
                        item {
                            GwenGlassCard(accentColor = GwenViolet) {
                                Text(
                                    text = "Hello Boss! Ask me to write code, research topics, translate Hindi/Hinglish/English, craft scripts, or analyze any image.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GwenTextPrimary
                                )
                            }
                        }
                    }

                    items(messages, key = { it.id }) { msg ->
                        val isUser = msg.role == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (isUser) 18.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 18.dp
                                ),
                                color = if (isUser) GwenElectricBlue.copy(alpha = 0.28f) else GwenSurfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (isUser) GwenElectricBlue.copy(alpha = 0.55f) else GwenCyan.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.widthIn(max = 310.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (isUser) "Boss" else "Gwen • ${msg.expression}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isUser) GwenElectricBlue else GwenCyan
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GwenTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (aiStreamState is AiStreamState.Thinking || aiStreamState is AiStreamState.Streaming) {
                        item {
                            GwenGlassCard(accentColor = GwenCyan) {
                                Text(
                                    text = if (aiStreamState is AiStreamState.Streaming) {
                                        aiStreamState.partialText
                                    } else {
                                        "Gwen is thinking..."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GwenTextPrimary
                                )
                            }
                        }
                    }

                    if (aiStreamState is AiStreamState.Error) {
                        item {
                            GwenGlassCard(accentColor = GwenDanger) {
                                Text(
                                    text = aiStreamState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GwenDanger
                                )
                            }
                        }
                    }
                }

                // Image Attachment Preview
                if (attachedBitmap != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = attachedBitmap!!.asImageBitmap(),
                            contentDescription = "Attached image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Image attached for Gemini Vision",
                            style = MaterialTheme.typography.bodySmall,
                            color = GwenCyan,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { attachedBitmap = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove image", tint = GwenDanger)
                        }
                    }
                }

                // Composer Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GwenObsidian)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.testTag("chat_attach_image_button")
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = stringResource(R.string.cd_pick_image),
                            tint = GwenCyan
                        )
                    }
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        placeholder = { Text("Message Gwen...", color = GwenTextMuted) },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GwenCyan,
                            unfocusedBorderColor = GwenGlassBorder,
                            focusedTextColor = GwenTextPrimary,
                            unfocusedTextColor = GwenTextPrimary,
                            focusedContainerColor = GwenSurface,
                            unfocusedContainerColor = GwenSurface
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_prompt_input")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (isSpeaking) {
                        IconButton(onClick = onStopSpeaking) {
                            Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.cd_stop_speaking), tint = GwenDanger)
                        }
                    } else {
                        IconButton(
                            onClick = onStartVoice,
                            modifier = Modifier.testTag("chat_voice_button")
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = stringResource(R.string.cd_mic_button),
                                tint = if (isListening) GwenDanger else GwenViolet
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            if (promptText.isNotBlank() || attachedBitmap != null) {
                                onSendPrompt(promptText, attachedBitmap)
                                promptText = ""
                                attachedBitmap = null
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GwenCyan)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.cd_send_message),
                            tint = GwenDeepBlack
                        )
                    }
                }
            }
        }
    }

    if (showHistorySheet) {
        val convs = (conversationsState as? DataUiState.Success)?.data.orEmpty()
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            containerColor = GwenObsidian
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Conversation History", style = MaterialTheme.typography.titleLarge, color = GwenCyan)
                if (convs.isEmpty()) {
                    Text("No saved conversations yet.", color = GwenTextSecondary)
                } else {
                    convs.forEach { conv ->
                        GwenGlassCard(
                            accentColor = if (conv.id == activeConversationId) GwenCyan else GwenGlassBorder,
                            onClick = {
                                onSelectConversation(conv.id)
                                showHistorySheet = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(conv.title, style = MaterialTheme.typography.titleSmall, color = GwenTextPrimary)
                            Text(
                                "${conv.mode.uppercase()} • ${conv.lastMessagePreview}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GwenTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
