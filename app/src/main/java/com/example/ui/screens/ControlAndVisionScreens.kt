package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.ContactItem
import com.example.core.DeviceController
import com.example.core.DeviceTelemetry
import com.example.core.GwenAccessibilityService
import com.example.core.GwenNotificationItem
import com.example.core.GwenSettings
import com.example.core.InstalledAppItem
import com.example.core.PermissionSnapshot
import com.example.core.ScreenUiElement
import com.example.data.model.AssistantTask
import com.example.data.model.TaskStage
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
import com.example.ui.theme.GwenObsidian
import com.example.ui.theme.GwenSuccess
import com.example.ui.theme.GwenSurface
import com.example.ui.theme.GwenTextMuted
import com.example.ui.theme.GwenTextPrimary
import com.example.ui.theme.GwenTextSecondary
import com.example.ui.theme.GwenViolet
import com.example.ui.theme.GwenWarning
import com.example.ui.viewmodel.DataUiState
import com.example.ui.viewmodel.ScreenVisionUiState

@Composable
fun PhoneControlCenterScreen(
    settings: GwenSettings,
    telemetry: DeviceTelemetry,
    permissions: PermissionSnapshot,
    installedApps: List<InstalledAppItem>,
    contacts: List<ContactItem>,
    notifications: List<GwenNotificationItem>,
    feedbackMessage: String?,
    deviceController: DeviceController,
    onSetVolume: (Int) -> Unit,
    onSetBrightness: (Int) -> Unit,
    onSearchContacts: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenPermissions: () -> Unit,
    onBack: () -> Unit,
    onBottomNav: (String) -> Unit
) {
    val context = LocalContext.current
    var appQuery by remember { mutableStateOf("") }
    var contactQuery by remember { mutableStateOf("") }
    var pendingSmsTarget by remember { mutableStateOf<ContactItem?>(null) }
    var pendingCallTarget by remember { mutableStateOf<ContactItem?>(null) }
    var smsDraftBody by remember { mutableStateOf("") }
    var sliderBrightness by remember(telemetry.brightnessPercent) {
        mutableFloatStateOf(telemetry.brightnessPercent.toFloat())
    }

    var contactsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        contactsGranted = granted
        onRefresh()
        if (granted) onSearchContacts(contactQuery)
    }

    val filteredApps = remember(installedApps, appQuery) {
        if (appQuery.isBlank()) installedApps.take(12)
        else installedApps.filter { it.label.lowercase().contains(appQuery.lowercase()) }.take(12)
    }

    Scaffold(
        containerColor = GwenDeepBlack,
        bottomBar = { GwenBottomNavBar(currentKey = "control", onSelect = onBottomNav) }
    ) { innerPadding ->
        GwenCosmicBackground(
            lightweightMode = settings.lightweightMode,
            modifier = Modifier.padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                GwenScreenTopBar(
                    title = "Phone Control Center",
                    subtitle = "Apps • Settings • Contacts • Accessibility",
                    onBack = onBack,
                    trailing = {
                        TextButton(onClick = onOpenPermissions) {
                            Text("Perms", color = GwenCyan)
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
                    if (feedbackMessage != null) {
                        GwenGlassCard(accentColor = GwenCyan) {
                            Text(feedbackMessage, style = MaterialTheme.typography.bodySmall, color = GwenCyan)
                        }
                    }

                    // Brightness & Volume Controls
                    GwenGlassCard(accentColor = GwenCyan) {
                        Text("Device Controls", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Media Volume: ${telemetry.mediaVolume} / ${telemetry.mediaMaxVolume}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GwenTextSecondary
                        )
                        Slider(
                            value = telemetry.mediaVolume.toFloat(),
                            onValueChange = { onSetVolume(it.toInt()) },
                            valueRange = 0f..telemetry.mediaMaxVolume.toFloat(),
                            colors = SliderDefaults.colors(thumbColor = GwenCyan, activeTrackColor = GwenCyan)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Screen Brightness: ${sliderBrightness.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = GwenTextSecondary
                            )
                            if (!permissions.writeSettings) {
                                TextButton(onClick = {
                                    deviceController.openSystemSettings(Settings.ACTION_MANAGE_WRITE_SETTINGS, withPackageUri = true)
                                }) {
                                    Text("Enable Write", color = GwenWarning)
                                }
                            }
                        }
                        Slider(
                            value = sliderBrightness,
                            onValueChange = { sliderBrightness = it },
                            onValueChangeFinished = { onSetBrightness(sliderBrightness.toInt()) },
                            valueRange = 10f..100f,
                            colors = SliderDefaults.colors(thumbColor = GwenViolet, activeTrackColor = GwenViolet)
                        )
                    }

                    // Accessibility & Notification Service Controls
                    GwenGlassCard(accentColor = GwenViolet) {
                        Text("System Automation Services", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = GwenCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Accessibility Control", style = MaterialTheme.typography.titleSmall, color = GwenTextPrimary)
                                    Text(
                                        if (permissions.accessibilityEnabled) "Connected & active" else "Tap to enable in Android Settings",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (permissions.accessibilityEnabled) GwenSuccess else GwenTextSecondary
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { deviceController.openSystemSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS) },
                                modifier = Modifier.testTag("control_open_accessibility_button")
                            ) {
                                Text(if (permissions.accessibilityEnabled) "Active" else "Enable", color = GwenCyan)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = GwenViolet)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Notification Access", style = MaterialTheme.typography.titleSmall, color = GwenTextPrimary)
                                    Text(
                                        if (permissions.notificationListenerEnabled) "${notifications.size} active notifications" else "Enable to read notifications",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (permissions.notificationListenerEnabled) GwenSuccess else GwenTextSecondary
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { deviceController.openSystemSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) }
                            ) {
                                Text(if (permissions.notificationListenerEnabled) "Active" else "Allow", color = GwenViolet)
                            }
                        }
                        if (permissions.accessibilityEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { GwenAccessibilityService.instance?.performScroll(true) }, modifier = Modifier.weight(1f)) {
                                    Text("Scroll Down", color = GwenTextPrimary)
                                }
                                OutlinedButton(onClick = { GwenAccessibilityService.instance?.goHome() }, modifier = Modifier.weight(1f)) {
                                    Text("Home", color = GwenTextPrimary)
                                }
                                OutlinedButton(onClick = { GwenAccessibilityService.instance?.openNotifications() }, modifier = Modifier.weight(1f)) {
                                    Text("Shade", color = GwenTextPrimary)
                                }
                            }
                        }
                    }

                    // Installed Apps Launcher
                    GwenGlassCard(accentColor = GwenElectricBlue) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Apps, contentDescription = null, tint = GwenElectricBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Launch Installed Apps", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = appQuery,
                            onValueChange = { appQuery = it },
                            placeholder = { Text("Search installed apps...", color = GwenTextMuted) },
                            singleLine = true,
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
                                .testTag("control_app_search_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (filteredApps.isEmpty()) {
                            Text("No matching launcher apps found.", style = MaterialTheme.typography.bodySmall, color = GwenTextSecondary)
                        } else {
                            filteredApps.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { app ->
                                        OutlinedButton(
                                            onClick = { deviceController.launchApp(app.packageName) },
                                            border = BorderStroke(1.dp, GwenGlassBorder),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Text(
                                                text = app.label,
                                                color = GwenTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }

                    // Contacts Search, Call & Confirmed SMS Drafting
                    GwenGlassCard(accentColor = GwenCyan) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Contacts, contentDescription = null, tint = GwenCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Contacts, Calls & Messages", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                            }
                            if (!contactsGranted) {
                                Button(
                                    onClick = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GwenCyan, contentColor = GwenDeepBlack)
                                ) {
                                    Text("Allow")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = contactQuery,
                                onValueChange = {
                                    contactQuery = it
                                    if (contactsGranted) onSearchContacts(it)
                                },
                                placeholder = { Text("Search contact name...", color = GwenTextMuted) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GwenCyan,
                                    unfocusedBorderColor = GwenGlassBorder,
                                    focusedTextColor = GwenTextPrimary,
                                    unfocusedTextColor = GwenTextPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (contactsGranted) onSearchContacts(contactQuery)
                                    else contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GwenElectricBlue)
                            ) {
                                Text("Find")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        contacts.take(5).forEach { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(contact.name, style = MaterialTheme.typography.titleSmall, color = GwenTextPrimary)
                                    Text(contact.phoneNumber, style = MaterialTheme.typography.bodySmall, color = GwenTextSecondary)
                                }
                                IconButton(onClick = { pendingCallTarget = contact }) {
                                    Icon(Icons.Default.Call, contentDescription = "Call ${contact.name}", tint = GwenSuccess)
                                }
                                IconButton(onClick = { pendingSmsTarget = contact }) {
                                    Icon(Icons.Default.Sms, contentDescription = "Message ${contact.name}", tint = GwenCyan)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Call Confirmation Dialog
    if (pendingCallTarget != null) {
        val target = pendingCallTarget!!
        AlertDialog(
            onDismissRequest = { pendingCallTarget = null },
            containerColor = GwenObsidian,
            title = { Text("Confirm Call", color = GwenTextPrimary) },
            text = { Text("Place call to ${target.name} (${target.phoneNumber})?", color = GwenTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        deviceController.initiateDirectCall(target.phoneNumber)
                        pendingCallTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GwenSuccess, contentColor = GwenDeepBlack)
                ) {
                    Text("Call Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCallTarget = null }) {
                    Text("Cancel", color = GwenTextSecondary)
                }
            }
        )
    }

    // SMS Draft & Confirmation Dialog
    if (pendingSmsTarget != null) {
        val target = pendingSmsTarget!!
        AlertDialog(
            onDismissRequest = { pendingSmsTarget = null },
            containerColor = GwenObsidian,
            title = { Text("Confirm Message to ${target.name}", color = GwenTextPrimary) },
            text = {
                OutlinedTextField(
                    value = smsDraftBody,
                    onValueChange = { smsDraftBody = it },
                    label = { Text("Message body") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = GwenTextPrimary,
                        unfocusedTextColor = GwenTextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deviceController.openSmsDraft(target.phoneNumber, smsDraftBody)
                        pendingSmsTarget = null
                        smsDraftBody = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GwenCyan, contentColor = GwenDeepBlack)
                ) {
                    Text("Open SMS App")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSmsTarget = null }) {
                    Text("Cancel", color = GwenTextSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreenUnderstandingScreen(
    settings: GwenSettings,
    visionState: ScreenVisionUiState,
    accessibilityConnected: Boolean,
    liveScreenNodes: List<ScreenUiElement>,
    onAnalyzeScreenshot: (Bitmap, String) -> Unit,
    onRefreshAccessibilityNodes: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var focusPrompt by remember { mutableStateOf("Read all visible text, identify buttons, and explain how to navigate this screen.") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (_: Exception) {
                null
            }
            if (bmp != null) {
                selectedBitmap = bmp
                onAnalyzeScreenshot(bmp, focusPrompt)
            }
        }
    }

    GwenCosmicBackground(lightweightMode = settings.lightweightMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            GwenScreenTopBar(
                title = "Screen Understanding",
                subtitle = "Gemini Vision OCR & Accessibility Tree",
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
                    Text("Screenshot Analyzer (Gemini Vision)", style = MaterialTheme.typography.titleMedium, color = GwenCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Pick any screenshot with the zero-permission Android Photo Picker for instant OCR, button detection, and navigation help.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GwenTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = focusPrompt,
                        onValueChange = { focusPrompt = it },
                        label = { Text("What should Gwen look for?") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GwenCyan,
                            unfocusedBorderColor = GwenGlassBorder,
                            focusedTextColor = GwenTextPrimary,
                            unfocusedTextColor = GwenTextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GwenCyan, contentColor = GwenDeepBlack),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("screen_pick_screenshot_button")
                    ) {
                        Icon(Icons.Default.ScreenshotMonitor, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick Screenshot & Analyze")
                    }

                    if (selectedBitmap != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Image(
                            bitmap = selectedBitmap!!.asImageBitmap(),
                            contentDescription = "Selected screenshot",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                }

                when (visionState) {
                    is ScreenVisionUiState.Analyzing -> {
                        GwenGlassCard(accentColor = GwenViolet) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = GwenCyan, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Running Gemini OCR & UI Element Detection...", color = GwenTextPrimary)
                            }
                        }
                    }
                    is ScreenVisionUiState.Error -> {
                        GwenGlassCard(accentColor = GwenDanger) {
                            Text(visionState.message, style = MaterialTheme.typography.bodySmall, color = GwenDanger)
                        }
                    }
                    is ScreenVisionUiState.Success -> {
                        val res = visionState.result
                        GwenGlassCard(accentColor = GwenSuccess) {
                            Text("Explanation", style = MaterialTheme.typography.titleMedium, color = GwenCyan)
                            Text(res.summary, style = MaterialTheme.typography.bodyMedium, color = GwenTextPrimary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Detected UI Buttons & Elements", style = MaterialTheme.typography.titleSmall, color = GwenViolet)
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                res.uiElements.forEach { el ->
                                    GwenStatusPill(label = el, color = GwenElectricBlue)
                                }
                            }
                            if (res.ocrText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Extracted OCR Text", style = MaterialTheme.typography.titleSmall, color = GwenCyan)
                                Text(res.ocrText, style = MaterialTheme.typography.bodySmall, color = GwenTextSecondary)
                            }
                            if (res.suggestedNextStep.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Recommended Next Step", style = MaterialTheme.typography.titleSmall, color = GwenSuccess)
                                Text(res.suggestedNextStep, style = MaterialTheme.typography.bodySmall, color = GwenTextPrimary)
                            }
                        }
                    }
                    is ScreenVisionUiState.Idle -> Unit
                }

                // Live Accessibility Reader
                GwenGlassCard(accentColor = GwenViolet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Live Accessibility Screen Reader", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                        GwenStatusPill(
                            label = if (accessibilityConnected) "ACTIVE" else "DISABLED",
                            color = if (accessibilityConnected) GwenSuccess else GwenWarning
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!accessibilityConnected) {
                        OutlinedButton(onClick = onOpenAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
                            Text("Enable Accessibility in Android Settings", color = GwenCyan)
                        }
                    } else {
                        Button(onClick = onRefreshAccessibilityNodes, modifier = Modifier.fillMaxWidth()) {
                            Text("Read Current Window Nodes (${liveScreenNodes.size})")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        liveScreenNodes.take(10).forEach { node ->
                            Text(
                                text = "• [${node.className}] ${node.text}${if (node.clickable) " (clickable)" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GwenTextSecondary
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
fun TaskManagerScreen(
    settings: GwenSettings,
    isEffectiveOnline: Boolean,
    activePipelineTask: AssistantTask?,
    tasksState: DataUiState<List<AssistantTask>>,
    onExecuteTask: (String) -> Unit,
    onBack: () -> Unit
) {
    var commandText by remember { mutableStateOf("") }
    val presets = listOf(
        "Open YouTube and search sci-fi ambient music",
        "Increase media volume",
        "Read visible screen elements",
        "Save project note: Review Galaxy A17 layout"
    )

    GwenCosmicBackground(lightweightMode = settings.lightweightMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            GwenScreenTopBar(
                title = "Task Execution Center",
                subtitle = "Verified 7-Stage Automation Pipeline",
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
                    Text("Run Verified Task", style = MaterialTheme.typography.titleMedium, color = GwenCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = commandText,
                            onValueChange = { commandText = it },
                            placeholder = { Text("Enter automation command...", color = GwenTextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GwenCyan,
                                unfocusedBorderColor = GwenGlassBorder,
                                focusedTextColor = GwenTextPrimary,
                                unfocusedTextColor = GwenTextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("task_command_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (commandText.isNotBlank()) {
                                    onExecuteTask(commandText)
                                    commandText = ""
                                }
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(GwenCyan)
                                .testTag("task_execute_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run task", tint = GwenDeepBlack)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    presets.forEach { preset ->
                        FilterChip(
                            selected = false,
                            onClick = { onExecuteTask(preset) },
                            label = { Text(preset, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Live 7-Stage Pipeline Card
                if (activePipelineTask != null) {
                    val stages = listOf(
                        TaskStage.UNDERSTANDING,
                        TaskStage.ANALYZING,
                        TaskStage.PLANNING,
                        TaskStage.CHECKING_PERMISSIONS,
                        TaskStage.EXECUTING,
                        TaskStage.VERIFYING
                    )
                    val currentIdx = when (activePipelineTask.stage) {
                        TaskStage.UNDERSTANDING.key -> 0
                        TaskStage.ANALYZING.key -> 1
                        TaskStage.PLANNING.key -> 2
                        TaskStage.CHECKING_PERMISSIONS.key -> 3
                        TaskStage.EXECUTING.key -> 4
                        TaskStage.VERIFYING.key -> 5
                        TaskStage.COMPLETED.key, TaskStage.FAILED.key -> 6
                        else -> 0
                    }
                    val isFailed = activePipelineTask.stage == TaskStage.FAILED.key
                    val isCompleted = activePipelineTask.stage == TaskStage.COMPLETED.key

                    GwenGlassCard(
                        accentColor = when {
                            isCompleted -> GwenSuccess
                            isFailed -> GwenDanger
                            else -> GwenElectricBlue
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("LIVE PIPELINE", style = MaterialTheme.typography.labelMedium, color = GwenCyan)
                            GwenStatusPill(
                                label = activePipelineTask.stage.uppercase(),
                                color = when {
                                    isCompleted -> GwenSuccess
                                    isFailed -> GwenDanger
                                    else -> GwenElectricBlue
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(activePipelineTask.command, style = MaterialTheme.typography.titleSmall, color = GwenTextPrimary)
                        Spacer(modifier = Modifier.height(10.dp))
                        stages.forEachIndexed { idx, stage ->
                            val done = idx < currentIdx || isCompleted
                            val active = idx == currentIdx && !isCompleted && !isFailed
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                done -> GwenSuccess
                                                active -> GwenCyan
                                                else -> GwenGlassBorder
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stage.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (done || active) GwenTextPrimary else GwenTextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isFailed) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = when {
                                    isCompleted -> GwenSuccess
                                    isFailed -> GwenDanger
                                    else -> GwenViolet
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activePipelineTask.verificationResult,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isFailed) GwenDanger else GwenTextPrimary
                            )
                        }
                    }
                }

                // Task History from Firestore
                val taskList = (tasksState as? DataUiState.Success)?.data.orEmpty()
                GwenGlassCard(accentColor = GwenViolet) {
                    Text(
                        text = if (isEffectiveOnline) "Verified Task History (${taskList.size})" else "Task History (Offline)",
                        style = MaterialTheme.typography.titleMedium,
                        color = GwenTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (taskList.isEmpty()) {
                        Text("No verified tasks logged yet.", style = MaterialTheme.typography.bodySmall, color = GwenTextSecondary)
                    } else {
                        taskList.take(10).forEach { task ->
                            val badgeColor = when (task.stage) {
                                TaskStage.COMPLETED.key -> GwenSuccess
                                TaskStage.FAILED.key -> GwenDanger
                                else -> GwenElectricBlue
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = task.command,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = GwenTextPrimary,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    GwenStatusPill(label = task.stage, color = badgeColor)
                                }
                                Text(
                                    text = task.verificationResult,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GwenTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
