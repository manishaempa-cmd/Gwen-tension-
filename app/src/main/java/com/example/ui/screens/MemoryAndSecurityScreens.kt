package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.GwenSettings
import com.example.data.local.LocalOfflineNote
import com.example.data.model.MemoryItem
import com.example.ui.components.GwenCosmicBackground
import com.example.ui.components.GwenGlassCard
import com.example.ui.components.GwenScreenTopBar
import com.example.ui.components.GwenStatusPill
import com.example.ui.theme.GwenCyan
import com.example.ui.theme.GwenDanger
import com.example.ui.theme.GwenDeepBlack
import com.example.ui.theme.GwenElectricBlue
import com.example.ui.theme.GwenGlassBorder
import com.example.ui.theme.GwenObsidian
import com.example.ui.theme.GwenSuccess
import com.example.ui.theme.GwenTextMuted
import com.example.ui.theme.GwenTextPrimary
import com.example.ui.theme.GwenTextSecondary
import com.example.ui.theme.GwenViolet
import com.example.ui.theme.GwenWarning
import com.example.ui.viewmodel.DataUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoryCenterScreen(
    settings: GwenSettings,
    isEffectiveOnline: Boolean,
    memoriesState: DataUiState<List<MemoryItem>>,
    localNotes: List<LocalOfflineNote>,
    onSaveMemory: (String, String, String) -> Unit,
    onUpdateMemory: (String, String, String, String) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onClearAllMemories: () -> Unit,
    onToggleMemoryEnabled: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingMemory by remember { mutableStateOf<MemoryItem?>(null) }
    var confirmClearAll by remember { mutableStateOf(false) }

    val cloudMemories = (memoriesState as? DataUiState.Success)?.data
    val displayItems: List<MemoryItem> = remember(cloudMemories, localNotes, isEffectiveOnline, searchQuery, selectedCategory) {
        val base = if (isEffectiveOnline && cloudMemories != null) {
            cloudMemories
        } else {
            localNotes.map {
                MemoryItem(
                    id = it.id,
                    title = it.title,
                    content = it.content,
                    category = it.category
                )
            }
        }
        base.filter { item ->
            val catOk = selectedCategory == "all" || item.category == selectedCategory
            val q = searchQuery.trim().lowercase()
            val searchOk = q.isBlank() ||
                item.title.lowercase().contains(q) ||
                item.content.lowercase().contains(q)
            catOk && searchOk
        }
    }

    GwenCosmicBackground(lightweightMode = settings.lightweightMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            GwenScreenTopBar(
                title = "Personal Memory Core",
                subtitle = if (isEffectiveOnline) "Synced via Firestore + Offline Cache" else "Offline Room Note Cache",
                onBack = onBack,
                trailing = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("memory_add_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add memory", tint = GwenCyan)
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GwenGlassCard(accentColor = GwenViolet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Personal Memory Injection", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                            Text(
                                if (settings.memoryEnabled) "Gwen uses approved memories during Gemini chats." else "Memory disabled for maximum privacy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GwenTextSecondary
                            )
                        }
                        Switch(
                            checked = settings.memoryEnabled,
                            onCheckedChange = onToggleMemoryEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = GwenCyan),
                            modifier = Modifier.testTag("memory_enabled_switch")
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GwenCyan) },
                    placeholder = { Text("Search stored memories...", color = GwenTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GwenCyan,
                        unfocusedBorderColor = GwenGlassBorder,
                        focusedTextColor = GwenTextPrimary,
                        unfocusedTextColor = GwenTextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("memory_search_input")
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all", "preference", "context", "note", "gaming").forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                if (displayItems.isEmpty()) {
                    GwenGlassCard(accentColor = GwenGlassBorder) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = GwenTextMuted)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "No saved memories match your filter. Tap + to teach Gwen a preference or project note.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GwenTextSecondary
                            )
                        }
                    }
                } else {
                    displayItems.forEach { item ->
                        GwenGlassCard(accentColor = GwenCyan) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.title, style = MaterialTheme.typography.titleSmall, color = GwenTextPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        GwenStatusPill(label = item.category.uppercase(), color = GwenViolet)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(item.content, style = MaterialTheme.typography.bodySmall, color = GwenTextSecondary)
                                }
                                Row {
                                    IconButton(onClick = { editingMemory = item }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit memory", tint = GwenCyan)
                                    }
                                    IconButton(onClick = { onDeleteMemory(item.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete memory", tint = GwenDanger)
                                    }
                                }
                            }
                        }
                    }
                }

                if (displayItems.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { confirmClearAll = true },
                        border = BorderStroke(1.dp, GwenDanger),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("memory_wipe_all_button")
                    ) {
                        Text("Wipe All Memories", color = GwenDanger)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showAddDialog || editingMemory != null) {
        val target = editingMemory
        var title by remember(target) { mutableStateOf(target?.title.orEmpty()) }
        var content by remember(target) { mutableStateOf(target?.content.orEmpty()) }
        var category by remember(target) { mutableStateOf(target?.category ?: "preference") }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingMemory = null
            },
            containerColor = GwenObsidian,
            title = {
                Text(if (target == null) "Store Personal Memory" else "Edit Memory", color = GwenTextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = GwenTextPrimary, unfocusedTextColor = GwenTextPrimary)
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Details for Gwen to remember") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = GwenTextPrimary, unfocusedTextColor = GwenTextPrimary)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("preference", "context", "note", "gaming").forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            if (target == null) {
                                onSaveMemory(title, content, category)
                            } else {
                                onUpdateMemory(target.id, title, content, category)
                            }
                            showAddDialog = false
                            editingMemory = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GwenCyan, contentColor = GwenDeepBlack)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    editingMemory = null
                }) {
                    Text("Cancel", color = GwenTextSecondary)
                }
            }
        )
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            containerColor = GwenObsidian,
            title = { Text("Delete All Memories?", color = GwenDanger) },
            text = { Text("This deletes all saved cloud and offline memories for your account.", color = GwenTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllMemories()
                        confirmClearAll = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GwenDanger)
                ) {
                    Text("Wipe All")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) {
                    Text("Cancel", color = GwenTextSecondary)
                }
            }
        )
    }
}

@Composable
fun OnlineOfflineModeScreen(
    settings: GwenSettings,
    systemNetworkOnline: Boolean,
    isEffectiveOnline: Boolean,
    onToggleForceOffline: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val onlineCapabilities = listOf(
        "Google Gemini AI streaming chat, reasoning & coding",
        "Multimodal Vision screenshot & image analysis",
        "Gemini Nano Banana 2.1 AI image generation & editing",
        "Cloud Firestore personal memory & history sync",
        "Structured Gemini intent planning in Task Center"
    )
    val offlineCapabilities = listOf(
        "Local \"Gwen\" wake-word & SpeechRecognizer command parsing",
        "Local acoustic voiceprint enrollment & replay verification",
        "On-device Android Text-to-Speech (female voice calibration)",
        "Installed app launcher & hardware volume/brightness controls",
        "Live battery/RAM telemetry & Room offline note lookup"
    )

    GwenCosmicBackground(lightweightMode = settings.lightweightMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            GwenScreenTopBar(
                title = "Online & Offline Matrix",
                subtitle = "Hybrid Neural Execution Engine",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GwenGlassCard(accentColor = if (isEffectiveOnline) GwenSuccess else GwenWarning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isEffectiveOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isEffectiveOnline) GwenSuccess else GwenWarning
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isEffectiveOnline) "Active Mode: ONLINE HYBRID" else "Active Mode: OFFLINE CORE",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GwenTextPrimary
                                )
                                Text(
                                    text = if (systemNetworkOnline) "Device network connected" else "No internet connection detected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GwenTextSecondary
                                )
                            }
                        }
                        GwenStatusPill(
                            label = if (isEffectiveOnline) "CLOUD" else "LOCAL",
                            color = if (isEffectiveOnline) GwenSuccess else GwenWarning
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Force Offline Core Mode", style = MaterialTheme.typography.titleSmall, color = GwenTextPrimary)
                            Text("Runs commands strictly using local device core & Room cache.", style = MaterialTheme.typography.bodySmall, color = GwenTextSecondary)
                        }
                        Switch(
                            checked = settings.forceOfflineMode,
                            onCheckedChange = onToggleForceOffline,
                            colors = SwitchDefaults.colors(checkedThumbColor = GwenWarning),
                            modifier = Modifier.testTag("force_offline_switch")
                        )
                    }
                }

                GwenGlassCard(accentColor = GwenCyan) {
                    Text("ONLINE FEATURES (Internet Required)", style = MaterialTheme.typography.titleSmall, color = GwenCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    onlineCapabilities.forEach { cap ->
                        Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = GwenCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cap, style = MaterialTheme.typography.bodySmall, color = GwenTextPrimary)
                        }
                    }
                }

                GwenGlassCard(accentColor = GwenViolet) {
                    Text("OFFLINE CAPABILITIES (No Internet Needed)", style = MaterialTheme.typography.titleSmall, color = GwenViolet)
                    Spacer(modifier = Modifier.height(8.dp))
                    offlineCapabilities.forEach { cap ->
                        Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GwenSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cap, style = MaterialTheme.typography.bodySmall, color = GwenTextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SecurityCenterScreen(
    settings: GwenSettings,
    userDisplayName: String,
    userEmail: String,
    onToggleReplayProtection: (Boolean) -> Unit,
    onToggleMemoryPrivacy: (Boolean) -> Unit,
    onSetAppPin: (Boolean, String) -> Unit,
    onOpenVoiceRegistration: () -> Unit,
    onOpenVoiceVerification: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    GwenCosmicBackground(lightweightMode = settings.lightweightMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            GwenScreenTopBar(
                title = "Security & Auth Center",
                subtitle = "Google Identity • Voiceprint • App PIN",
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GwenCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(userDisplayName.ifBlank { "Boss" }, style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                            Text(userEmail.ifBlank { "Google Account Active" }, style = MaterialTheme.typography.bodySmall, color = GwenTextSecondary)
                        }
                        GwenStatusPill(label = "SIGNED IN", color = GwenSuccess)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onSignOut,
                        border = BorderStroke(1.dp, GwenDanger),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("security_sign_out_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = GwenDanger)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out of Google Account", color = GwenDanger)
                    }
                }

                GwenGlassCard(accentColor = GwenViolet) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = GwenViolet)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Owner Voice Biometrics", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (settings.voicePrintEnrolled) {
                            "Enrolled • Last verification similarity: ${settings.lastVerifiedScore}%"
                        } else {
                            "Owner voiceprint not enrolled yet."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (settings.voicePrintEnrolled) GwenSuccess else GwenWarning
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenVoiceRegistration, modifier = Modifier.weight(1f)) {
                            Text("Enroll", color = GwenCyan)
                        }
                        Button(
                            onClick = onOpenVoiceVerification,
                            colors = ButtonDefaults.buttonColors(containerColor = GwenViolet),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Verify Live")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Replay-Attack Protection", style = MaterialTheme.typography.bodyMedium, color = GwenTextPrimary)
                            Text("Detects static audio playback spoofing.", style = MaterialTheme.typography.bodySmall, color = GwenTextSecondary)
                        }
                        Switch(
                            checked = settings.replayProtectionEnabled,
                            onCheckedChange = onToggleReplayProtection
                        )
                    }
                }

                GwenGlassCard(accentColor = GwenElectricBlue) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = GwenElectricBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("App PIN Lock", style = MaterialTheme.typography.titleSmall, color = GwenTextPrimary)
                                Text(
                                    if (settings.securityPinEnabled) "4-digit PIN lock active on startup" else "Tap to configure startup PIN",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GwenTextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = settings.securityPinEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) showPinDialog = true else onSetAppPin(false, "")
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Personal Memory Storage", style = MaterialTheme.typography.titleSmall, color = GwenTextPrimary)
                            Text("Allow Gwen to persist approved user context.", style = MaterialTheme.typography.bodySmall, color = GwenTextSecondary)
                        }
                        Switch(
                            checked = settings.memoryEnabled,
                            onCheckedChange = onToggleMemoryPrivacy
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = GwenObsidian,
            title = { Text("Set 4-Digit Security PIN", color = GwenTextPrimary) },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pinInput = it },
                    label = { Text("Enter 4-6 digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = GwenTextPrimary, unfocusedTextColor = GwenTextPrimary)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length >= 4) {
                            onSetAppPin(true, pinInput)
                            pinInput = ""
                            showPinDialog = false
                        }
                    },
                    enabled = pinInput.length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = GwenCyan, contentColor = GwenDeepBlack)
                ) {
                    Text("Enable PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel", color = GwenTextSecondary)
                }
            }
        )
    }
}
