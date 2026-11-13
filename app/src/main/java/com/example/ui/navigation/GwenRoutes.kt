package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface GwenRoute {
    @Serializable data object Splash : GwenRoute
    @Serializable data object Welcome : GwenRoute
    @Serializable data object InitialSetup : GwenRoute
    @Serializable data object VoiceRegistration : GwenRoute
    @Serializable data object VoiceVerification : GwenRoute
    @Serializable data object Dashboard : GwenRoute
    @Serializable data object AiChat : GwenRoute
    @Serializable data object AvatarStudio : GwenRoute
    @Serializable data object VoiceStudio : GwenRoute
    @Serializable data object PhoneControl : GwenRoute
    @Serializable data object ScreenUnderstanding : GwenRoute
    @Serializable data object TaskManager : GwenRoute
    @Serializable data object ImageVideoStudio : GwenRoute
    @Serializable data object GamingAssistant : GwenRoute
    @Serializable data object MusicCenter : GwenRoute
    @Serializable data object MemoryCenter : GwenRoute
    @Serializable data object OnlineOfflineMode : GwenRoute
    @Serializable data object SecurityCenter : GwenRoute
    @Serializable data object PermissionsCenter : GwenRoute
    @Serializable data object Settings : GwenRoute
    @Serializable data object AiStatusDashboard : GwenRoute
}
