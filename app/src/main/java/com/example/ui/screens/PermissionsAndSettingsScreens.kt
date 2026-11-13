package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.example.BuildConfig
import com.example.R
import com.example.core.AppCheckDiagnosticResult
import com.example.core.DeviceController
import com.example.core.DeviceTelemetry
import com.example.core.GwenSettings
import com.example.core.PermissionSnapshot
import com.example.core.getStoredDebugSecret
import com.example.ui.components.GwenBottomNavBar
import com.example.ui.components.GwenCosmicBackground
import com.example.ui.components.GwenGlassCard
import com.example.ui.components.GwenScreenTopBar
import com.example.ui.components.GwenStatusPill
import com.example.ui.navigation.GwenRoute
import com.example.ui.theme.GwenCyan
import com.example.ui.theme.GwenDanger
import com.example.ui.theme.GwenDeepBlack
import com.example.ui.theme.GwenElectricBlue
import com.example.ui.theme.GwenGlassBorder
import com.example.ui.theme.GwenSuccess
import com.example.ui.theme.GwenTextMuted
import com.example.ui.theme.GwenTextPrimary
import com.example.ui.theme.GwenTextSecondary
import com.example.ui.theme.GwenViolet
import com.example.ui.theme.GwenWarning

@Composable
fun PermissionsCenterScreen(
    settings: GwenSettings,
    permissions: PermissionSnapshot,
    deviceController: DeviceController,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    val runtimeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        onRefresh()
    }

    GwenCosmicBackground(lightweightMode = settings.lightweightMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            GwenScreenTopBar(
                title = "Permissions Center",
                subtitle = "Least-Privilege System Access",
                onBack = onBack,
                trailing = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh permissions", tint = GwenCyan)
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
                PermissionRowCard(
                    title = "Microphone (RECORD_AUDIO)",
                    description = "Voice commands, \"Gwen\" wake word & owner voiceprint capture.",
                    granted = permissions.recordAudio,
                    actionLabel = "Grant",
                    onAction = { runtimeLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )
                PermissionRowCard(
                    title = "Contacts (READ_CONTACTS)",
                    description = "Search contacts for user-confirmed calling & SMS drafting.",
                    granted = permissions.readContacts,
                    actionLabel = "Grant",
                    onAction = { runtimeLauncher.launch(Manifest.permission.READ_CONTACTS) }
                )
                PermissionRowCard(
                    title = "Phone Calls (CALL_PHONE)",
                    description = "Optional direct call initiation after confirmation dialog.",
                    granted = permissions.callPhone,
                    actionLabel = "Grant",
                    onAction = { runtimeLauncher.launch(Manifest.permission.CALL_PHONE) }
                )
                PermissionRowCard(
                    title = "Camera (CAMERA)",
                    description = "Optional live camera capture for multimodal vision.",
                    granted = permissions.camera,
                    actionLabel = "Grant",
                    onAction = { runtimeLauncher.launch(Manifest.permission.CAMERA) }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionRowCard(
                        title = "Notifications (POST_NOTIFICATIONS)",
                        description = "Required for battery-friendly foreground wake service.",
                        granted = permissions.postNotifications,
                        actionLabel = "Grant",
                        onAction = { runtimeLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    )
                }
                PermissionRowCard(
                    title = "Modify System Settings (Brightness)",
                    description = "Allows Gwen to set screen brightness on command.",
                    granted = permissions.writeSettings,
                    actionLabel = "Settings",
                    onAction = {
                        deviceController.openSystemSettings(Settings.ACTION_MANAGE_WRITE_SETTINGS, withPackageUri = true)
                    }
                )
                PermissionRowCard(
                    title = "Accessibility Service",
                    description = "Reads visible UI nodes and performs user-approved scroll/navigation.",
                    granted = permissions.accessibilityEnabled,
                    actionLabel = "Settings",
                    onAction = { deviceController.openSystemSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) }
                )
                PermissionRowCard(
                    title = "Notification Listener Access",
                    description = "Lets Gwen summarize active device notifications on request.",
                    granted = permissions.notificationListenerEnabled,
                    actionLabel = "Settings",
                    onAction = { deviceController.openSystemSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) }
                )
                PermissionRowCard(
                    title = "Battery Optimization Exemption",
                    description = "Helps background wake listening stay responsive when screen is active.",
                    granted = permissions.ignoringBatteryOptimizations,
                    actionLabel = "Configure",
                    onAction = { deviceController.openSystemSettings(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PermissionRowCard(
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    GwenGlassCard(accentColor = if (granted) GwenSuccess else GwenWarning) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = GwenTextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = GwenTextSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (granted) {
                GwenStatusPill(label = "GRANTED", color = GwenSuccess)
            } else {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = GwenCyan, contentColor = GwenDeepBlack)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: GwenSettings,
    wakeServiceRunning: Boolean,
    onUpdateSettings: ((GwenSettings) -> GwenSettings) -> Unit,
    onToggleWakeService: (Boolean) -> Unit,
    onOpenRoute: (GwenRoute) -> Unit,
    onBack: () -> Unit,
    onBottomNav: (String) -> Unit
) {
    val models = listOf(
        "gemini-3.8-flash" to "Gemini 3.8 Flash",
        "gemini-3.5-flash-lite" to "3.5 Flash Lite",
        "gemini-4-argon" to "Gemini 4 Argon"
    )

    Scaffold(
        containerColor = GwenDeepBlack,
        bottomBar = { GwenBottomNavBar(currentKey = "settings", onSelect = onBottomNav) }
    ) { innerPadding ->
        GwenCosmicBackground(
            lightweightMode = settings.lightweightMode,
            modifier = Modifier.padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                GwenScreenTopBar(
                    title = "Advanced Settings",
                    subtitle = "Gwen AI – Maximum Edition",
                    onBack = onBack
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick Sub-Screens Navigation
                    GwenGlassCard(accentColor = GwenCyan) {
                        Text("Control Modules & Customization", style = MaterialTheme.typography.titleMedium, color = GwenCyan)
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsNavButton("Avatar Customization", Icons.Default.Face, GwenCyan) {
                            onOpenRoute(GwenRoute.AvatarStudio)
                        }
                        SettingsNavButton("Voice Settings & Calibration", Icons.Default.GraphicEq, GwenViolet) {
                            onOpenRoute(GwenRoute.VoiceStudio)
                        }
                        SettingsNavButton("Owner Voice Registration", Icons.Default.Security, GwenElectricBlue) {
                            onOpenRoute(GwenRoute.VoiceRegistration)
                        }
                        SettingsNavButton("Security, Auth & PIN Lock", Icons.Default.Shield, GwenSuccess) {
                            onOpenRoute(GwenRoute.SecurityCenter)
                        }
                        SettingsNavButton("Online / Offline Hybrid Mode", Icons.Default.CloudDone, GwenCyan) {
                            onOpenRoute(GwenRoute.OnlineOfflineMode)
                        }
                        SettingsNavButton("Memory & Privacy Management", Icons.Default.Memory, GwenViolet) {
                            onOpenRoute(GwenRoute.MemoryCenter)
                        }
                        SettingsNavButton("Accessibility & System Permissions", Icons.Default.Devices, GwenWarning) {
                            onOpenRoute(GwenRoute.PermissionsCenter)
                        }
                        SettingsNavButton("AI Connection & Diagnostics", Icons.Default.MonitorHeart, GwenCyan) {
                            onOpenRoute(GwenRoute.AiStatusDashboard)
                        }
                    }

                    // Gemini AI Model & Configuration
                    GwenGlassCard(accentColor = GwenViolet) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = GwenViolet)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini AI Brain & Language", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Authenticated securely via Firebase AI Logic & App Check (zero client API key exposure).",
                            style = MaterialTheme.typography.bodySmall,
                            color = GwenTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            models.forEach { (id, label) ->
                                FilterChip(
                                    selected = settings.preferredModel == id,
                                    onClick = { onUpdateSettings { it.copy(preferredModel = id) } },
                                    label = { Text(label) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Language", style = MaterialTheme.typography.labelMedium, color = GwenTextSecondary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("en-IN" to "English", "hi-IN" to "Hindi", "hinglish" to "Hinglish").forEach { (code, label) ->
                                FilterChip(
                                    selected = settings.preferredLanguage == code,
                                    onClick = { onUpdateSettings { it.copy(preferredLanguage = code) } },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    // Background, Wake Word, Theme & Battery Optimization
                    GwenGlassCard(accentColor = GwenElectricBlue) {
                        Text("Wake Service, Performance & Battery", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingSwitchRow(
                            title = "Background \"Gwen\" Service",
                            subtitle = "Foreground microphone listener notification.",
                            checked = wakeServiceRunning,
                            onCheckedChange = onToggleWakeService
                        )
                        Text(
                            "Wake Word Sensitivity: ${(settings.wakeWordSensitivity * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = GwenTextSecondary
                        )
                        Slider(
                            value = settings.wakeWordSensitivity,
                            onValueChange = { v -> onUpdateSettings { it.copy(wakeWordSensitivity = v) } },
                            valueRange = 0.3f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = GwenCyan, activeTrackColor = GwenCyan)
                        )
                        SettingSwitchRow(
                            title = "6GB RAM Lightweight Theme",
                            subtitle = "Streamlines holographic gradients for Galaxy A17 5G.",
                            checked = settings.lightweightMode,
                            onCheckedChange = { enabled -> onUpdateSettings { it.copy(lightweightMode = enabled) } }
                        )
                        SettingSwitchRow(
                            title = "Battery-Friendly Optimization",
                            subtitle = "Throttles idle animations when battery saver is active.",
                            checked = settings.batteryFriendlyMode,
                            onCheckedChange = { enabled -> onUpdateSettings { it.copy(batteryFriendlyMode = enabled) } }
                        )
                        SettingSwitchRow(
                            title = "Personal Memory Enabled",
                            subtitle = "Controls persistent Firestore & local note context.",
                            checked = settings.memoryEnabled,
                            onCheckedChange = { enabled -> onUpdateSettings { it.copy(memoryEnabled = enabled) } }
                        )
                    }

                    // About Gwen AI
                    GwenGlassCard(accentColor = GwenGlassBorder) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = GwenCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("About GWEN AI – MAXIMUM EDITION", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Version ${BuildConfig.VERSION_NAME} • Built with Jetpack Compose, Material 3, Firebase AI Logic, Cloud Firestore, and Room Hybrid Storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GwenTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsNavButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, GwenGlassBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, color = GwenTextPrimary)
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = GwenTextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = GwenTextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = GwenCyan)
        )
    }
}

@Composable
fun AiStatusDashboardScreen(
    settings: GwenSettings,
    isEffectiveOnline: Boolean,
    systemNetworkOnline: Boolean,
    lastLatencyMs: Long?,
    wakeServiceRunning: Boolean,
    isListening: Boolean,
    isSpeaking: Boolean,
    userUid: String,
    telemetry: DeviceTelemetry,
    appCheckDiagnostic: AppCheckDiagnosticResult?,
    geminiPingStatus: String?,
    isDiagnosing: Boolean,
    onVerifyAppCheck: () -> Unit,
    onTestGeminiConnection: (String) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val dbId = context.getString(R.string.firestore_database_id)
    var copiedDebugToken by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onVerifyAppCheck()
    }

    val storedDebugSecret = remember(appCheckDiagnostic) {
        appCheckDiagnostic?.debugSecretUuid ?: getStoredDebugSecret(context)
    }

    GwenCosmicBackground(lightweightMode = settings.lightweightMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            GwenScreenTopBar(
                title = "AI Status Dashboard",
                subtitle = "Real-Time Neural & Firebase Diagnostics",
                onBack = onBack,
                trailing = {
                    IconButton(
                        onClick = {
                            onRefresh()
                            onVerifyAppCheck()
                        },
                        modifier = Modifier.testTag("status_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh status", tint = GwenCyan)
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
                GwenGlassCard(accentColor = GwenCyan) {
                    Text("Firebase AI Logic & App Check", style = MaterialTheme.typography.titleMedium, color = GwenCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    DiagnosticRow("Primary Model", settings.preferredModel)
                    DiagnosticRow(
                        "App Check Provider",
                        appCheckDiagnostic?.providerName ?: if (BuildConfig.DEBUG) "DebugAppCheckProviderFactory" else "RecaptchaAppCheckProviderFactory"
                    )
                    DiagnosticRow(
                        "App Check Token",
                        when {
                            isDiagnosing && appCheckDiagnostic == null -> "Verifying..."
                            appCheckDiagnostic == null -> "Not checked"
                            appCheckDiagnostic.tokenValid -> "Verified"
                            else -> "Invalid"
                        }
                    )
                    DiagnosticRow("Effective Network", if (isEffectiveOnline) "Online Hybrid" else "Offline Core")
                    DiagnosticRow("System Connectivity", if (systemNetworkOnline) "Connected" else "Disconnected")
                    DiagnosticRow("Last Gemini Latency", lastLatencyMs?.let { "${it} ms" } ?: "No inference yet")

                    if (appCheckDiagnostic != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = appCheckDiagnostic.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (appCheckDiagnostic.tokenValid) GwenSuccess else GwenDanger
                        )
                    }

                    if (BuildConfig.DEBUG && !storedDebugSecret.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (copiedDebugToken) "Debug Token UUID (Copied)" else "Device Debug Token UUID",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (copiedDebugToken) GwenSuccess else GwenWarning
                                )
                                Text(
                                    text = storedDebugSecret,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = GwenTextPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(storedDebugSecret))
                                    copiedDebugToken = true
                                },
                                modifier = Modifier.testTag("copy_debug_token_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Debug Token UUID",
                                    tint = GwenCyan
                                )
                            }
                        }
                    }

                    if (!geminiPingStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = geminiPingStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (geminiPingStatus.startsWith("Connected")) GwenSuccess else GwenWarning
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onVerifyAppCheck,
                            enabled = !isDiagnosing,
                            border = BorderStroke(1.dp, GwenCyan),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("verify_app_check_button")
                        ) {
                            Text("Verify App Check", color = GwenCyan, maxLines = 1)
                        }
                        Button(
                            onClick = { onTestGeminiConnection(settings.preferredModel) },
                            enabled = !isDiagnosing && isEffectiveOnline,
                            colors = ButtonDefaults.buttonColors(containerColor = GwenCyan, contentColor = GwenDeepBlack),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("test_gemini_button")
                        ) {
                            if (isDiagnosing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = GwenDeepBlack
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Test Gemini", maxLines = 1)
                        }
                    }
                }

                GwenGlassCard(accentColor = GwenViolet) {
                    Text("Cloud Firestore & Identity", style = MaterialTheme.typography.titleMedium, color = GwenViolet)
                    Spacer(modifier = Modifier.height(8.dp))
                    DiagnosticRow("Auth User UID", userUid.take(14) + "...")
                    DiagnosticRow("Database ID", dbId)
                    DiagnosticRow("Memory Sync", if (settings.memoryEnabled) "Active" else "Paused")
                }

                GwenGlassCard(accentColor = GwenElectricBlue) {
                    Text("Voice & Hardware Telemetry", style = MaterialTheme.typography.titleMedium, color = GwenElectricBlue)
                    Spacer(modifier = Modifier.height(8.dp))
                    DiagnosticRow("Wake Service", if (wakeServiceRunning) "Running (Foreground)" else "Standby")
                    DiagnosticRow("Speech Recognizer", if (isListening) "Listening" else "Idle")
                    DiagnosticRow("Female TTS Engine", if (isSpeaking) "Speaking" else "Ready")
                    DiagnosticRow("Device Model", telemetry.deviceModel)
                    DiagnosticRow("Available RAM", "${telemetry.ramAvailableMb} MB / ${telemetry.ramTotalMb} MB")
                    DiagnosticRow("Battery & Temp", "${telemetry.batteryPercent}% • ${telemetry.batteryTempC}°C")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = GwenTextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = GwenTextPrimary)
    }
}
