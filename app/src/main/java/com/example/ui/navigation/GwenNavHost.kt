package com.example.ui.navigation

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.core.GwenAccessibilityService
import com.example.core.GwenNotificationListenerService
import com.example.core.GwenWakeWordService
import com.example.data.model.AiChatMode
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.AiStatusDashboardScreen
import com.example.ui.screens.AvatarStudioScreen
import com.example.ui.screens.GamingAssistantScreen
import com.example.ui.screens.ImageVideoStudioScreen
import com.example.ui.screens.InitialSetupScreen
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.MemoryCenterScreen
import com.example.ui.screens.MusicCenterScreen
import com.example.ui.screens.OnlineOfflineModeScreen
import com.example.ui.screens.PermissionsCenterScreen
import com.example.ui.screens.PhoneControlCenterScreen
import com.example.ui.screens.ScreenUnderstandingScreen
import com.example.ui.screens.SecurityCenterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TaskManagerScreen
import com.example.ui.screens.VoiceRegistrationScreen
import com.example.ui.screens.VoiceStudioScreen
import com.example.ui.screens.VoiceVerificationScreen
import com.example.ui.screens.WelcomeAuthScreen
import com.example.ui.viewmodel.GwenAssistantViewModel
import com.example.ui.viewmodel.GwenToolsViewModel

@Composable
fun GwenNavHost(
    userUid: String,
    userDisplayName: String,
    userEmail: String,
    assistantViewModel: GwenAssistantViewModel,
    toolsViewModel: GwenToolsViewModel,
    onSignOut: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val settings by assistantViewModel.settings.collectAsStateWithLifecycle()
    val systemNetworkOnline by assistantViewModel.systemNetworkOnline.collectAsStateWithLifecycle()
    val isEffectiveOnline by assistantViewModel.isEffectiveOnline.collectAsStateWithLifecycle()
    val activeConversationId by assistantViewModel.activeConversationId.collectAsStateWithLifecycle()
    val chatMode by assistantViewModel.chatMode.collectAsStateWithLifecycle()
    val expression by assistantViewModel.expression.collectAsStateWithLifecycle()
    val liveStatus by assistantViewModel.liveStatus.collectAsStateWithLifecycle()
    val aiStreamState by assistantViewModel.aiStreamState.collectAsStateWithLifecycle()
    val lastLatencyMs by assistantViewModel.lastLatencyMs.collectAsStateWithLifecycle()
    val enrollmentStatus by assistantViewModel.enrollmentStatus.collectAsStateWithLifecycle()
    val verificationResult by assistantViewModel.verificationResult.collectAsStateWithLifecycle()
    val conversationsState by assistantViewModel.conversationsState.collectAsStateWithLifecycle()
    val messagesState by assistantViewModel.messagesState.collectAsStateWithLifecycle()
    val memoriesState by assistantViewModel.memoriesState.collectAsStateWithLifecycle()
    val localNotes by assistantViewModel.localOfflineNotes.collectAsStateWithLifecycle()

    val isSpeaking by assistantViewModel.voiceEngine.isSpeaking.collectAsStateWithLifecycle()
    val isListening by assistantViewModel.voiceEngine.isListening.collectAsStateWithLifecycle()
    val rmsLevel by assistantViewModel.voiceEngine.rmsLevel.collectAsStateWithLifecycle()
    val partialTranscript by assistantViewModel.voiceEngine.partialText.collectAsStateWithLifecycle()
    val voiceError by assistantViewModel.voiceEngine.voiceError.collectAsStateWithLifecycle()

    val tasksState by toolsViewModel.tasksState.collectAsStateWithLifecycle()
    val galleryMedia by toolsViewModel.galleryMedia.collectAsStateWithLifecycle()
    val imageState by toolsViewModel.imageStudioState.collectAsStateWithLifecycle()
    val visionState by toolsViewModel.screenVisionState.collectAsStateWithLifecycle()
    val installedApps by toolsViewModel.installedApps.collectAsStateWithLifecycle()
    val contacts by toolsViewModel.contacts.collectAsStateWithLifecycle()
    val telemetry by toolsViewModel.telemetry.collectAsStateWithLifecycle()
    val permissions by toolsViewModel.permissions.collectAsStateWithLifecycle()
    val activePipelineTask by toolsViewModel.activePipelineTask.collectAsStateWithLifecycle()
    val actionFeedback by toolsViewModel.actionFeedback.collectAsStateWithLifecycle()
    val appCheckDiagnostic by toolsViewModel.appCheckDiagnostic.collectAsStateWithLifecycle()
    val geminiPingStatus by toolsViewModel.geminiPingStatus.collectAsStateWithLifecycle()
    val isDiagnosing by toolsViewModel.isDiagnosing.collectAsStateWithLifecycle()

    val accessibilityConnected by GwenAccessibilityService.isConnected.collectAsStateWithLifecycle()
    val liveScreenNodes by GwenAccessibilityService.screenElements.collectAsStateWithLifecycle()
    val activeNotifications by GwenNotificationListenerService.notifications.collectAsStateWithLifecycle()
    val wakeServiceRunning by GwenWakeWordService.isRunning.collectAsStateWithLifecycle()

    val navigateToDashboard: () -> Unit = {
        navController.navigate(GwenRoute.Dashboard) {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }

    val handleBottomNav: (String) -> Unit = { key ->
        val target: GwenRoute = when (key) {
            "dashboard" -> GwenRoute.Dashboard
            "chat" -> GwenRoute.AiChat
            "control" -> GwenRoute.PhoneControl
            "studio" -> GwenRoute.ImageVideoStudio
            "settings" -> GwenRoute.Settings
            else -> GwenRoute.Dashboard
        }
        navController.navigate(target) {
            popUpTo(GwenRoute.Dashboard) { inclusive = false }
            launchSingleTop = true
        }
    }

    val startDestination: GwenRoute = if (settings.setupCompleted) GwenRoute.Dashboard else GwenRoute.Welcome

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<GwenRoute.Splash> {
            SplashScreen(onFinished = {
                if (settings.setupCompleted) navigateToDashboard()
                else navController.navigate(GwenRoute.Welcome) {
                    popUpTo(GwenRoute.Splash) { inclusive = true }
                }
            })
        }

        composable<GwenRoute.Welcome> {
            WelcomeAuthScreen(
                isSignedIn = true,
                onContinueToSetup = { navController.navigate(GwenRoute.InitialSetup) },
                onContinueToDashboard = {
                    assistantViewModel.updateSettings { it.copy(setupCompleted = true) }
                    navigateToDashboard()
                }
            )
        }

        composable<GwenRoute.InitialSetup> {
            InitialSetupScreen(
                settings = settings,
                onUpdateSettings = { transform -> assistantViewModel.updateSettings(transform) },
                onGoVoiceRegistration = { navController.navigate(GwenRoute.VoiceRegistration) },
                onCompleteSetup = navigateToDashboard,
                onBack = { navController.popBackStack() }
            )
        }

        composable<GwenRoute.VoiceRegistration> {
            VoiceRegistrationScreen(
                settings = settings,
                rmsLevel = rmsLevel,
                enrollmentStatus = enrollmentStatus,
                onEnrollVoice = { assistantViewModel.enrollOwnerVoicePrint() },
                onGoVerify = { navController.navigate(GwenRoute.VoiceVerification) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<GwenRoute.VoiceVerification> {
            VoiceVerificationScreen(
                settings = settings,
                rmsLevel = rmsLevel,
                verificationResult = verificationResult,
                onVerifyVoice = { assistantViewModel.verifyOwnerVoicePrint() },
                onToggleReplayProtection = { enabled ->
                    assistantViewModel.updateSettings { it.copy(replayProtectionEnabled = enabled) }
                },
                onDone = {
                    assistantViewModel.updateSettings { it.copy(setupCompleted = true) }
                    navigateToDashboard()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<GwenRoute.Dashboard> {
            MainDashboardScreen(
                settings = settings,
                expression = expression,
                liveStatus = liveStatus,
                isEffectiveOnline = isEffectiveOnline,
                isSpeaking = isSpeaking,
                isListening = isListening,
                rmsLevel = rmsLevel,
                partialTranscript = partialTranscript,
                voiceError = voiceError,
                aiStreamState = aiStreamState,
                conversationsState = conversationsState,
                messagesState = messagesState,
                activeConversationId = activeConversationId,
                onSelectConversation = { assistantViewModel.selectConversation(it) },
                onNewConversation = { assistantViewModel.startNewConversation() },
                onStartVoice = {
                    assistantViewModel.startVoiceCommand(wakeWordOnly = settings.wakeWordEnabled)
                },
                onStopVoice = { assistantViewModel.stopVoiceInput() },
                onStopSpeaking = { assistantViewModel.stopSpeaking() },
                onSendPrompt = { prompt -> assistantViewModel.sendPrompt(prompt) },
                onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
                onBottomNav = handleBottomNav
            )
        }

        composable<GwenRoute.AiChat> {
            AiChatScreen(
                settings = settings,
                chatMode = chatMode,
                isEffectiveOnline = isEffectiveOnline,
                isSpeaking = isSpeaking,
                isListening = isListening,
                aiStreamState = aiStreamState,
                conversationsState = conversationsState,
                messagesState = messagesState,
                activeConversationId = activeConversationId,
                onSelectMode = { assistantViewModel.setChatMode(it) },
                onSelectConversation = { assistantViewModel.selectConversation(it) },
                onNewConversation = { assistantViewModel.startNewConversation() },
                onStartVoice = { assistantViewModel.startVoiceCommand(wakeWordOnly = false) },
                onStopSpeaking = { assistantViewModel.stopSpeaking() },
                onSendPrompt = { prompt, bmp -> assistantViewModel.sendPrompt(prompt, bmp) },
                onBack = { navController.popBackStack() },
                onBottomNav = handleBottomNav
            )
        }

        composable<GwenRoute.AvatarStudio> {
            AvatarStudioScreen(
                settings = settings,
                expression = expression,
                isSpeaking = isSpeaking,
                rmsLevel = rmsLevel,
                onSelectAvatarMode = { assistantViewModel.setAvatarMode(it) },
                onSelectExpression = { assistantViewModel.setExpression(it) },
                onPreviewSpeech = { assistantViewModel.previewFemaleVoice() },
                onStopSpeech = { assistantViewModel.stopSpeaking() },
                onToggleLightweight = { enabled ->
                    assistantViewModel.updateSettings { it.copy(lightweightMode = enabled) }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<GwenRoute.VoiceStudio> {
            VoiceStudioScreen(
                settings = settings,
                isSpeaking = isSpeaking,
                isListening = isListening,
                rmsLevel = rmsLevel,
                wakeServiceRunning = wakeServiceRunning,
                onUpdateSettings = { transform -> assistantViewModel.updateSettings(transform) },
                onPreviewVoice = { assistantViewModel.previewFemaleVoice() },
                onStopSpeaking = { assistantViewModel.stopSpeaking() },
                onTestWakeListening = { assistantViewModel.startVoiceCommand(wakeWordOnly = true) },
                onToggleBackgroundService = { assistantViewModel.toggleWakeService(it) },
                onOpenRegistration = { navController.navigate(GwenRoute.VoiceRegistration) },
                onOpenVerification = { navController.navigate(GwenRoute.VoiceVerification) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<GwenRoute.PhoneControl> {
            PhoneControlCenterScreen(
                settings = settings,
                telemetry = telemetry,
                permissions = permissions,
                installedApps = installedApps,
                contacts = contacts,
                notifications = activeNotifications,
                feedbackMessage = actionFeedback,
                deviceController = assistantViewModel.deviceController,
                onSetVolume = { toolsViewModel.setMediaVolume(it) },
                onSetBrightness = { toolsViewModel.setSystemBrightness(it) },
                onSearchContacts = { toolsViewModel.searchContacts(it) },
                onRefresh = { toolsViewModel.refreshSystemState() },
                onOpenPermissions = { navController.navigate(GwenRoute.PermissionsCenter) },
                onBack = { navController.popBackStack() },
                onBottomNav = handleBottomNav
            )
        }

        composable<GwenRoute.ScreenUnderstanding> {
            ScreenUnderstandingScreen(
                settings = settings,
                visionState = visionState,
                accessibilityConnected = accessibilityConnected,
                liveScreenNodes = liveScreenNodes,
                onAnalyzeScreenshot = { bmp, focus -> toolsViewModel.analyzeScreenBitmap(bmp, focus) },
                onRefreshAccessibilityNodes = { GwenAccessibilityService.instance?.refreshScreenNodes() },
                onOpenAccessibilitySettings = {
                    assistantViewModel.deviceController.openSystemSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<GwenRoute.TaskManager> {
            TaskManagerScreen(
                settings = settings,
                isEffectiveOnline = isEffectiveOnline,
                activePipelineTask = activePipelineTask,
                tasksState = tasksState,
                onExecuteTask = { cmd -> toolsViewModel.runVerifiedTask(cmd, isEffectiveOnline) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<GwenRoute.ImageVideoStudio> {
            ImageVideoStudioScreen(
                settings = settings,
                isEffectiveOnline = isEffectiveOnline,
                imageState = imageState,
                galleryMedia = galleryMedia,
                onGenerateImage = { prompt, bmp -> toolsViewModel.generateStudioImage(prompt, bmp) },
                onDeleteMedia = { toolsViewModel.deleteGeneratedMedia(it) },
                onAskCreativeAi = { scriptPrompt ->
                    assistantViewModel.setChatMode(AiChatMode.STORY)
                    assistantViewModel.sendPrompt(scriptPrompt)
                    navController.navigate(GwenRoute.AiChat)
                },
                onBack = { navController.popBackStack() },
                onBottomNav = handleBottomNav
            )
        }

        composable<GwenRoute.GamingAssistant> {
            GamingAssistantScreen(
                settings = settings,
                telemetry = telemetry,
                isEffectiveOnline = isEffectiveOnline,
                onSaveHudNotes = { notes ->
                    assistantViewModel.updateSettings { it.copy(hudSensitivityNotes = notes) }
                    assistantViewModel.saveMemory("Gaming HUD & Sensitivity", notes, "gaming")
                },
                onRefreshTelemetry = { toolsViewModel.refreshSystemState() },
                onAskGamingAi = { query ->
                    assistantViewModel.setChatMode(AiChatMode.GAMING)
                    assistantViewModel.sendPrompt(query)
                    navController.navigate(GwenRoute.AiChat)
                },
                onToggleLightweight = { enabled ->
                    assistantViewModel.updateSettings { it.copy(lightweightMode = enabled) }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<GwenRoute.MusicCenter> {
            MusicCenterScreen(
                settings = settings,
                telemetry = telemetry,
                onPlayPause = { toolsViewModel.mediaPlayPause() },
                onNext = { toolsViewModel.mediaNext() },
                onPrevious = { toolsViewModel.mediaPrevious() },
                onSetVolume = { toolsViewModel.setMediaVolume(it) },
                onOpenYouTubeSearch = { query ->
                    assistantViewModel.deviceController.openYouTubeSearch(query)
                },
                onRefresh = { toolsViewModel.refreshSystemState() },
                onBack = { navController.popBackStack() }
            )
        }

        composable<GwenRoute.MemoryCenter> {
            MemoryCenterScreen(
                settings = settings,
                isEffectiveOnline = isEffectiveOnline,
                memoriesState = memoriesState,
                localNotes = localNotes,
                onSaveMemory = { title, content, cat -> assistantViewModel.saveMemory(title, content, cat) },
                onUpdateMemory = { id, title, content, cat -> assistantViewModel.updateMemory(id, title, content, cat) },
                onDeleteMemory = { assistantViewModel.deleteMemory(it) },
                onClearAllMemories = { assistantViewModel.clearAllMemories() },
                onToggleMemoryEnabled = { enabled ->
                    assistantViewModel.updateSettings { it.copy(memoryEnabled = enabled) }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<GwenRoute.OnlineOfflineMode> {
            OnlineOfflineModeScreen(
                settings = settings,
                systemNetworkOnline = systemNetworkOnline,
                isEffectiveOnline = isEffectiveOnline,
                onToggleForceOffline = { forced ->
                    assistantViewModel.updateSettings { it.copy(forceOfflineMode = forced) }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<GwenRoute.SecurityCenter> {
            SecurityCenterScreen(
                settings = settings,
                userDisplayName = userDisplayName,
                userEmail = userEmail,
                onToggleReplayProtection = { enabled ->
                    assistantViewModel.updateSettings { it.copy(replayProtectionEnabled = enabled) }
                },
                onToggleMemoryPrivacy = { enabled ->
                    assistantViewModel.updateSettings { it.copy(memoryEnabled = enabled) }
                },
                onSetAppPin = { enabled, pin ->
                    assistantViewModel.updateSettings {
                        it.copy(securityPinEnabled = enabled, securityPinCode = if (enabled) pin else "")
                    }
                },
                onOpenVoiceRegistration = { navController.navigate(GwenRoute.VoiceRegistration) },
                onOpenVoiceVerification = { navController.navigate(GwenRoute.VoiceVerification) },
                onSignOut = onSignOut,
                onBack = { navController.popBackStack() }
            )
        }

        composable<GwenRoute.PermissionsCenter> {
            PermissionsCenterScreen(
                settings = settings,
                permissions = permissions,
                deviceController = assistantViewModel.deviceController,
                onRefresh = { toolsViewModel.refreshSystemState() },
                onBack = { navController.popBackStack() }
            )
        }

        composable<GwenRoute.Settings> {
            SettingsScreen(
                settings = settings,
                wakeServiceRunning = wakeServiceRunning,
                onUpdateSettings = { transform -> assistantViewModel.updateSettings(transform) },
                onToggleWakeService = { assistantViewModel.toggleWakeService(it) },
                onOpenRoute = { route -> navController.navigate(route) { launchSingleTop = true } },
                onBack = { navController.popBackStack() },
                onBottomNav = handleBottomNav
            )
        }

        composable<GwenRoute.AiStatusDashboard> {
            AiStatusDashboardScreen(
                settings = settings,
                isEffectiveOnline = isEffectiveOnline,
                systemNetworkOnline = systemNetworkOnline,
                lastLatencyMs = lastLatencyMs,
                wakeServiceRunning = wakeServiceRunning,
                isListening = isListening,
                isSpeaking = isSpeaking,
                userUid = userUid,
                telemetry = telemetry,
                appCheckDiagnostic = appCheckDiagnostic,
                geminiPingStatus = geminiPingStatus,
                isDiagnosing = isDiagnosing,
                onVerifyAppCheck = { toolsViewModel.runAppCheckVerification() },
                onTestGeminiConnection = { toolsViewModel.runGeminiConnectionTest(settings.preferredModel) },
                onRefresh = { toolsViewModel.refreshSystemState() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
