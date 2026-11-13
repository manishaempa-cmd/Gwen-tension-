package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import com.example.R
import com.example.core.GwenSettings
import com.example.core.VoiceVerificationResult
import com.example.core.attemptAutoSignIn
import com.example.core.onGoogleSignInClicked
import com.example.data.model.AvatarMode
import com.example.data.model.GwenExpression
import com.example.ui.components.GwenAvatarStage
import com.example.ui.components.GwenCosmicBackground
import com.example.ui.components.GwenGlassCard
import com.example.ui.components.GwenScreenTopBar
import com.example.ui.components.GwenStatusPill
import com.example.ui.components.VoiceWaveformVisualizer
import com.example.ui.theme.GwenCyan
import com.example.ui.theme.GwenDanger
import com.example.ui.theme.GwenDeepBlack
import com.example.ui.theme.GwenElectricBlue
import com.example.ui.theme.GwenSuccess
import com.example.ui.theme.GwenTextPrimary
import com.example.ui.theme.GwenTextSecondary
import com.example.ui.theme.GwenViolet
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "splashAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.86f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "splashScale"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(1300)
        onFinished()
    }

    GwenCosmicBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .alpha(alpha)
                    .scale(scale)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(164.dp)
                        .clip(CircleShape)
                        .border(2.dp, Brush.linearGradient(listOf(GwenCyan, GwenViolet)), CircleShape)
                        .padding(6.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_gwen_upper_body),
                        contentDescription = stringResource(R.string.cd_gwen_avatar),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "GWEN AI",
                    style = MaterialTheme.typography.displayLarge,
                    color = GwenTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                GwenStatusPill(
                    label = "MAXIMUM EDITION • NEURAL CORE",
                    color = GwenCyan
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Initializing Holographic Assistant & App Check...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GwenTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun WelcomeAuthScreen(
    isSignedIn: Boolean,
    onContinueToSetup: () -> Unit,
    onContinueToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!isSignedIn) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { },
                onUnauthenticated = { },
                scope = scope
            )
        }
    }

    GwenCosmicBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GwenStatusPill(label = "NEXT-GEN PERSONAL AI", color = GwenViolet)
                    Spacer(modifier = Modifier.height(14.dp))
                    GwenAvatarStage(
                        avatarMode = AvatarMode.UPPER_BODY,
                        expression = GwenExpression.HAPPY,
                        isSpeaking = false,
                        isListening = false,
                        rmsLevel = 0f,
                        lightweightMode = false,
                        size = 200.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.welcome_greeting),
                        style = MaterialTheme.typography.displayMedium,
                        color = GwenCyan,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "I'm Gwen, your futuristic Android companion powered by Google Gemini AI, holographic vision, and verified device control.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GwenTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                GwenGlassCard(accentColor = GwenCyan) {
                    FeatureRow("Gemini AI Reasoning & Vision", "Multi-turn streaming, code, translation & screen OCR")
                    Spacer(modifier = Modifier.height(10.dp))
                    FeatureRow("Hinglish, Hindi & English Voice", "Owner voiceprint verification & wake-word guard")
                    Spacer(modifier = Modifier.height(10.dp))
                    FeatureRow("Encrypted Personal Memory", "Synced to your Google account via Firestore")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (authError != null) {
                        Text(
                            text = authError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = GwenDanger,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    if (!isSignedIn) {
                        Button(
                            onClick = {
                                authError = null
                                isSigningIn = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = { isSigningIn = false },
                                    onAuthError = { msg ->
                                        isSigningIn = false
                                        authError = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = { isSigningIn = false }
                                )
                            },
                            enabled = !isSigningIn,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GwenCyan,
                                contentColor = GwenDeepBlack
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = GwenDeepBlack,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.sign_in_google),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onContinueToSetup,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GwenCyan,
                                contentColor = GwenDeepBlack
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("welcome_continue_setup_button")
                        ) {
                            Text("Initialize Setup", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onContinueToDashboard,
                            border = BorderStroke(1.dp, GwenViolet),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("welcome_skip_to_dashboard_button")
                        ) {
                            Text("Enter Gwen Dashboard", color = GwenTextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = GwenCyan,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = GwenTextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = GwenTextSecondary)
        }
    }
}

@Composable
fun InitialSetupScreen(
    settings: GwenSettings,
    onUpdateSettings: ((GwenSettings) -> GwenSettings) -> Unit,
    onGoVoiceRegistration: () -> Unit,
    onCompleteSetup: () -> Unit,
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
                title = "Initial Setup",
                subtitle = "Step 1 of 2 • Configure Core",
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
                        Icon(Icons.Default.Language, contentDescription = null, tint = GwenCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Assistant Language", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("en-IN" to "English", "hi-IN" to "Hindi", "hinglish" to "Hinglish").forEach { (code, label) ->
                            val selected = settings.preferredLanguage == code
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateSettings { it.copy(preferredLanguage = code) } },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GwenCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = GwenCyan
                                ),
                                modifier = Modifier.testTag("setup_lang_$code")
                            )
                        }
                    }
                }

                GwenGlassCard(accentColor = GwenViolet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Microphone & Voice Recognition", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                            Text(
                                text = if (micGranted) "Permission granted for voice & wake word." else "Required for speaking with Gwen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GwenTextSecondary
                            )
                        }
                        Button(
                            onClick = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            enabled = !micGranted,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (micGranted) GwenSuccess else GwenViolet
                            ),
                            modifier = Modifier.testTag("setup_request_mic_button")
                        ) {
                            Text(if (micGranted) "Granted" else "Allow")
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
                            Text("6GB RAM Lightweight Mode", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                            Text(
                                "Reduces particle shaders for extra-smooth performance on mid-range devices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GwenTextSecondary
                            )
                        }
                        Switch(
                            checked = settings.lightweightMode,
                            onCheckedChange = { enabled -> onUpdateSettings { it.copy(lightweightMode = enabled) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = GwenCyan)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Battery-Friendly Background", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                            Text(
                                "Keeps wake listening low-power.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GwenTextSecondary
                            )
                        }
                        Switch(
                            checked = settings.batteryFriendlyMode,
                            onCheckedChange = { enabled -> onUpdateSettings { it.copy(batteryFriendlyMode = enabled) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = GwenCyan)
                        )
                    }
                }

                Button(
                    onClick = onGoVoiceRegistration,
                    colors = ButtonDefaults.buttonColors(containerColor = GwenViolet),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("setup_enroll_voice_button")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Proceed to Owner Voice Registration")
                }

                OutlinedButton(
                    onClick = {
                        onUpdateSettings { it.copy(setupCompleted = true) }
                        onCompleteSetup()
                    },
                    border = BorderStroke(1.dp, GwenCyan),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("setup_finish_button")
                ) {
                    Text("Finish & Enter Dashboard", color = GwenCyan)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun VoiceRegistrationScreen(
    settings: GwenSettings,
    rmsLevel: Float,
    enrollmentStatus: String?,
    onEnrollVoice: () -> Unit,
    onGoVerify: () -> Unit,
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
        if (granted) onEnrollVoice()
    }

    GwenCosmicBackground(lightweightMode = settings.lightweightMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            GwenScreenTopBar(
                title = "Voice Registration",
                subtitle = "Owner Acoustic Voiceprint",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GwenGlassCard(accentColor = GwenCyan) {
                    Text(
                        text = "Enroll Your Voiceprint",
                        style = MaterialTheme.typography.titleLarge,
                        color = GwenCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap Record below and say clearly for 2.5 seconds:\n\"Gwen, recognize my voice and unlock maximum mode.\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GwenTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    VoiceWaveformVisualizer(
                        rmsLevel = rmsLevel,
                        isActive = rmsLevel > 0.02f,
                        isSpeaking = false,
                        lightweightMode = settings.lightweightMode
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    if (enrollmentStatus != null) {
                        Text(
                            text = enrollmentStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = GwenSuccess
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Button(
                        onClick = {
                            if (micGranted) onEnrollVoice() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GwenCyan,
                            contentColor = GwenDeepBlack
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("voice_enroll_record_button")
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (settings.voicePrintEnrolled) "Re-Enroll Owner Voice" else "Capture Voiceprint"
                        )
                    }
                }

                GwenGlassCard(accentColor = GwenViolet) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = GwenViolet)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (settings.voicePrintEnrolled) "Status: Owner Voiceprint Active" else "Status: Not Yet Enrolled",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (settings.voicePrintEnrolled) GwenSuccess else GwenTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your acoustic signature (zero-crossing rate, spectral balance, and live modulation contour) stays stored privately on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GwenTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onGoVerify,
                        border = BorderStroke(1.dp, GwenCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("go_to_voice_verification_button")
                    ) {
                        Text("Test Voice Verification", color = GwenCyan)
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceVerificationScreen(
    settings: GwenSettings,
    rmsLevel: Float,
    verificationResult: VoiceVerificationResult?,
    onVerifyVoice: () -> Unit,
    onToggleReplayProtection: (Boolean) -> Unit,
    onDone: () -> Unit,
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
        if (granted) onVerifyVoice()
    }

    GwenCosmicBackground(lightweightMode = settings.lightweightMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            GwenScreenTopBar(
                title = "Voice Verification",
                subtitle = "Speaker ID & Replay Guard",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GwenGlassCard(accentColor = GwenElectricBlue) {
                    Text(
                        text = "Live Challenge Verification",
                        style = MaterialTheme.typography.titleLarge,
                        color = GwenCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Speak your verification phrase to test acoustic match and anti-spoofing energy dynamics.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GwenTextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    VoiceWaveformVisualizer(
                        rmsLevel = rmsLevel,
                        isActive = rmsLevel > 0.02f,
                        isSpeaking = false,
                        lightweightMode = settings.lightweightMode
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (verificationResult != null) {
                        val badgeColor = if (verificationResult.matched) GwenSuccess else GwenDanger
                        GwenStatusPill(
                            label = if (verificationResult.matched) {
                                "MATCH ${verificationResult.similarityPercent}% • LIVE"
                            } else {
                                "REJECTED ${verificationResult.similarityPercent}%"
                            },
                            color = badgeColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = verificationResult.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = GwenTextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = {
                            if (micGranted) onVerifyVoice() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GwenElectricBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("verify_voice_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Live Voice Check")
                    }
                }

                GwenGlassCard(accentColor = GwenViolet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Replay-Attack Protection", style = MaterialTheme.typography.titleMedium, color = GwenTextPrimary)
                            Text(
                                "Rejects flat-gain pre-recorded speaker playback by verifying live dynamic modulation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GwenTextSecondary
                            )
                        }
                        Switch(
                            checked = settings.replayProtectionEnabled,
                            onCheckedChange = onToggleReplayProtection,
                            colors = SwitchDefaults.colors(checkedThumbColor = GwenCyan)
                        )
                    }
                }

                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GwenCyan,
                        contentColor = GwenDeepBlack
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("verification_done_button")
                ) {
                    Text("Continue to Gwen Dashboard")
                }
            }
        }
    }
}
