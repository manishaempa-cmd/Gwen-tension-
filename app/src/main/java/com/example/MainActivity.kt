package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.DeviceController
import com.example.core.NetworkMonitor
import com.example.core.PreferencesStore
import com.example.core.VoiceEngine
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.core.signOutFromGwen
import com.example.data.local.GwenLocalDatabase
import com.example.data.repository.GeminiAiRepository
import com.example.data.repository.GwenFirestoreRepository
import com.example.ui.components.GwenCosmicBackground
import com.example.ui.components.GwenGlassCard
import com.example.ui.navigation.GwenNavHost
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WelcomeAuthScreen
import com.example.ui.theme.GwenAiTheme
import com.example.ui.theme.GwenCyan
import com.example.ui.theme.GwenDanger
import com.example.ui.theme.GwenDeepBlack
import com.example.ui.theme.GwenGlassBorder
import com.example.ui.theme.GwenTextPrimary
import com.example.ui.theme.GwenTextSecondary
import com.example.ui.viewmodel.GwenAssistantViewModel
import com.example.ui.viewmodel.GwenToolsViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val appContext = applicationContext

        setContent {
            GwenAiTheme {
                val scope = rememberCoroutineScope()
                val credentialManager = remember(this) { CredentialManager.create(this) }
                val currentUser by remember { Firebase.auth.authStateFlow() }
                    .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

                var splashFinished by remember { mutableStateOf(false) }

                if (!splashFinished) {
                    SplashScreen(onFinished = { splashFinished = true })
                } else {
                    val user = currentUser
                    if (user == null) {
                        WelcomeAuthScreen(
                            isSignedIn = false,
                            onContinueToSetup = {},
                            onContinueToDashboard = {}
                        )
                    } else {
                        key(user.uid) {
                            val firestoreRepo = remember(user.uid) { GwenFirestoreRepository(appContext) }
                            val geminiRepo = remember { GeminiAiRepository() }
                            val localDb = remember { GwenLocalDatabase.getInstance(appContext) }
                            val preferencesStore = remember { PreferencesStore(appContext) }
                            val networkMonitor = remember { NetworkMonitor(appContext) }
                            val voiceEngine = remember(user.uid) { VoiceEngine(appContext) }
                            val deviceController = remember { DeviceController(appContext) }

                            val assistantViewModel: GwenAssistantViewModel = viewModel(
                                key = "assistant_${user.uid}",
                                factory = GwenAssistantViewModel.provideFactory(
                                    currentUserId = user.uid,
                                    userDisplayName = user.displayName ?: "Boss",
                                    firestoreRepo = firestoreRepo,
                                    geminiRepo = geminiRepo,
                                    localDb = localDb,
                                    preferencesStore = preferencesStore,
                                    networkMonitor = networkMonitor,
                                    voiceEngine = voiceEngine,
                                    deviceController = deviceController
                                )
                            )

                            val toolsViewModel: GwenToolsViewModel = viewModel(
                                key = "tools_${user.uid}",
                                factory = GwenToolsViewModel.provideFactory(
                                    appContext = appContext,
                                    currentUserId = user.uid,
                                    firestoreRepo = firestoreRepo,
                                    geminiRepo = geminiRepo,
                                    localDb = localDb,
                                    networkMonitor = networkMonitor,
                                    deviceController = deviceController
                                )
                            )

                            val settings by assistantViewModel.settings.collectAsStateWithLifecycle()
                            var pinUnlocked by remember(user.uid) { mutableStateOf(false) }

                            val doSignOut = {
                                signOutFromGwen(
                                    credentialManager = credentialManager,
                                    scope = scope
                                )
                            }

                            if (settings.securityPinEnabled && settings.securityPinCode.isNotBlank() && !pinUnlocked) {
                                PinLockScreen(
                                    expectedPin = settings.securityPinCode,
                                    lightweightMode = settings.lightweightMode,
                                    onUnlocked = { pinUnlocked = true },
                                    onSignOut = doSignOut
                                )
                            } else {
                                GwenNavHost(
                                    userUid = user.uid,
                                    userDisplayName = user.displayName ?: "Boss",
                                    userEmail = user.email.orEmpty(),
                                    assistantViewModel = assistantViewModel,
                                    toolsViewModel = toolsViewModel,
                                    onSignOut = doSignOut
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }
}

@Composable
private fun PinLockScreen(
    expectedPin: String,
    lightweightMode: Boolean,
    onUnlocked: () -> Unit,
    onSignOut: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    GwenCosmicBackground(lightweightMode = lightweightMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GwenGlassCard(
                accentColor = GwenCyan,
                modifier = Modifier.widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GWEN SECURITY LOCK",
                        style = MaterialTheme.typography.titleLarge,
                        color = GwenCyan
                    )
                    Text(
                        text = "Enter your Gwen Security PIN to unlock assistant memory and device controls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GwenTextSecondary
                    )
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            if (it.length <= 6 && it.all(Char::isDigit)) {
                                enteredPin = it
                                pinError = false
                            }
                        },
                        label = { Text("Security PIN") },
                        visualTransformation = PasswordVisualTransformation(),
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
                            .testTag("pin_lock_input")
                    )
                    if (pinError) {
                        Text("Incorrect PIN. Please try again.", style = MaterialTheme.typography.bodySmall, color = GwenDanger)
                    }
                    Button(
                        onClick = {
                            if (enteredPin == expectedPin) {
                                onUnlocked()
                            } else {
                                pinError = true
                                enteredPin = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GwenCyan, contentColor = GwenDeepBlack),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pin_unlock_button")
                    ) {
                        Text("Unlock Gwen AI")
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out", color = GwenDanger)
                    }
                }
            }
        }
    }
}
