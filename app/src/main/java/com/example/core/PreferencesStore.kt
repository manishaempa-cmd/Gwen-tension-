package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AvatarMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gwen_preferences")

data class GwenSettings(
    val setupCompleted: Boolean = false,
    val avatarMode: AvatarMode = AvatarMode.UPPER_BODY,
    val lightweightMode: Boolean = false,
    val preferredLanguage: String = "en-IN",
    val preferredModel: String = "gemini-3.8-flash",
    val voicePitch: Float = 1.12f,
    val voiceSpeed: Float = 1.02f,
    val voiceVolume: Float = 0.9f,
    val autoSpeakResponses: Boolean = true,
    val wakeWordEnabled: Boolean = false,
    val wakeWordSensitivity: Float = 0.72f,
    val backgroundListeningEnabled: Boolean = false,
    val batteryFriendlyMode: Boolean = true,
    val forceOfflineMode: Boolean = false,
    val memoryEnabled: Boolean = true,
    val replayProtectionEnabled: Boolean = true,
    val securityPinEnabled: Boolean = false,
    val securityPinCode: String = "",
    val voicePrintEnrolled: Boolean = false,
    val voicePrintSignature: String = "",
    val lastVerifiedScore: Int = 0,
    val hudSensitivityNotes: String = "Camera: 115% | ADS: 78% | Gyro: 220% | 4-Finger Claw HUD"
)

class PreferencesStore(private val context: Context) {
    private object Keys {
        val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
        val AVATAR_MODE = stringPreferencesKey("avatar_mode")
        val LIGHTWEIGHT_MODE = booleanPreferencesKey("lightweight_mode")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val PREFERRED_MODEL = stringPreferencesKey("preferred_model")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val VOICE_VOLUME = floatPreferencesKey("voice_volume")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val WAKE_SENSITIVITY = floatPreferencesKey("wake_sensitivity")
        val BACKGROUND_LISTENING = booleanPreferencesKey("background_listening")
        val BATTERY_FRIENDLY = booleanPreferencesKey("battery_friendly")
        val FORCE_OFFLINE = booleanPreferencesKey("force_offline")
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val REPLAY_PROTECTION = booleanPreferencesKey("replay_protection")
        val SECURITY_PIN_ENABLED = booleanPreferencesKey("security_pin_enabled")
        val SECURITY_PIN_CODE = stringPreferencesKey("security_pin_code")
        val VOICEPRINT_ENROLLED = booleanPreferencesKey("voiceprint_enrolled")
        val VOICEPRINT_SIGNATURE = stringPreferencesKey("voiceprint_signature")
        val LAST_VERIFIED_SCORE = intPreferencesKey("last_verified_score")
        val HUD_SENSITIVITY_NOTES = stringPreferencesKey("hud_sensitivity_notes")
    }

    val settingsFlow: Flow<GwenSettings> = context.dataStore.data.map { prefs ->
        val avatarModeName = prefs[Keys.AVATAR_MODE] ?: AvatarMode.UPPER_BODY.name
        val avatarMode = AvatarMode.entries.firstOrNull { it.name == avatarModeName } ?: AvatarMode.UPPER_BODY
        GwenSettings(
            setupCompleted = prefs[Keys.SETUP_COMPLETED] ?: false,
            avatarMode = avatarMode,
            lightweightMode = prefs[Keys.LIGHTWEIGHT_MODE] ?: false,
            preferredLanguage = prefs[Keys.PREFERRED_LANGUAGE] ?: "en-IN",
            preferredModel = prefs[Keys.PREFERRED_MODEL] ?: "gemini-3.8-flash",
            voicePitch = prefs[Keys.VOICE_PITCH] ?: 1.12f,
            voiceSpeed = prefs[Keys.VOICE_SPEED] ?: 1.02f,
            voiceVolume = prefs[Keys.VOICE_VOLUME] ?: 0.9f,
            autoSpeakResponses = prefs[Keys.AUTO_SPEAK] ?: true,
            wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: false,
            wakeWordSensitivity = prefs[Keys.WAKE_SENSITIVITY] ?: 0.72f,
            backgroundListeningEnabled = prefs[Keys.BACKGROUND_LISTENING] ?: false,
            batteryFriendlyMode = prefs[Keys.BATTERY_FRIENDLY] ?: true,
            forceOfflineMode = prefs[Keys.FORCE_OFFLINE] ?: false,
            memoryEnabled = prefs[Keys.MEMORY_ENABLED] ?: true,
            replayProtectionEnabled = prefs[Keys.REPLAY_PROTECTION] ?: true,
            securityPinEnabled = prefs[Keys.SECURITY_PIN_ENABLED] ?: false,
            securityPinCode = prefs[Keys.SECURITY_PIN_CODE] ?: "",
            voicePrintEnrolled = prefs[Keys.VOICEPRINT_ENROLLED] ?: false,
            voicePrintSignature = prefs[Keys.VOICEPRINT_SIGNATURE] ?: "",
            lastVerifiedScore = prefs[Keys.LAST_VERIFIED_SCORE] ?: 0,
            hudSensitivityNotes = prefs[Keys.HUD_SENSITIVITY_NOTES]
                ?: "Camera: 115% | ADS: 78% | Gyro: 220% | 4-Finger Claw HUD"
        )
    }

    suspend fun update(transform: (GwenSettings) -> GwenSettings) {
        context.dataStore.edit { prefs ->
            val current = GwenSettings(
                setupCompleted = prefs[Keys.SETUP_COMPLETED] ?: false,
                avatarMode = AvatarMode.entries.firstOrNull { it.name == prefs[Keys.AVATAR_MODE] }
                    ?: AvatarMode.UPPER_BODY,
                lightweightMode = prefs[Keys.LIGHTWEIGHT_MODE] ?: false,
                preferredLanguage = prefs[Keys.PREFERRED_LANGUAGE] ?: "en-IN",
                preferredModel = prefs[Keys.PREFERRED_MODEL] ?: "gemini-3.8-flash",
                voicePitch = prefs[Keys.VOICE_PITCH] ?: 1.12f,
                voiceSpeed = prefs[Keys.VOICE_SPEED] ?: 1.02f,
                voiceVolume = prefs[Keys.VOICE_VOLUME] ?: 0.9f,
                autoSpeakResponses = prefs[Keys.AUTO_SPEAK] ?: true,
                wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: false,
                wakeWordSensitivity = prefs[Keys.WAKE_SENSITIVITY] ?: 0.72f,
                backgroundListeningEnabled = prefs[Keys.BACKGROUND_LISTENING] ?: false,
                batteryFriendlyMode = prefs[Keys.BATTERY_FRIENDLY] ?: true,
                forceOfflineMode = prefs[Keys.FORCE_OFFLINE] ?: false,
                memoryEnabled = prefs[Keys.MEMORY_ENABLED] ?: true,
                replayProtectionEnabled = prefs[Keys.REPLAY_PROTECTION] ?: true,
                securityPinEnabled = prefs[Keys.SECURITY_PIN_ENABLED] ?: false,
                securityPinCode = prefs[Keys.SECURITY_PIN_CODE] ?: "",
                voicePrintEnrolled = prefs[Keys.VOICEPRINT_ENROLLED] ?: false,
                voicePrintSignature = prefs[Keys.VOICEPRINT_SIGNATURE] ?: "",
                lastVerifiedScore = prefs[Keys.LAST_VERIFIED_SCORE] ?: 0,
                hudSensitivityNotes = prefs[Keys.HUD_SENSITIVITY_NOTES]
                    ?: "Camera: 115% | ADS: 78% | Gyro: 220% | 4-Finger Claw HUD"
            )
            val next = transform(current)
            prefs[Keys.SETUP_COMPLETED] = next.setupCompleted
            prefs[Keys.AVATAR_MODE] = next.avatarMode.name
            prefs[Keys.LIGHTWEIGHT_MODE] = next.lightweightMode
            prefs[Keys.PREFERRED_LANGUAGE] = next.preferredLanguage
            prefs[Keys.PREFERRED_MODEL] = next.preferredModel
            prefs[Keys.VOICE_PITCH] = next.voicePitch
            prefs[Keys.VOICE_SPEED] = next.voiceSpeed
            prefs[Keys.VOICE_VOLUME] = next.voiceVolume
            prefs[Keys.AUTO_SPEAK] = next.autoSpeakResponses
            prefs[Keys.WAKE_WORD_ENABLED] = next.wakeWordEnabled
            prefs[Keys.WAKE_SENSITIVITY] = next.wakeWordSensitivity
            prefs[Keys.BACKGROUND_LISTENING] = next.backgroundListeningEnabled
            prefs[Keys.BATTERY_FRIENDLY] = next.batteryFriendlyMode
            prefs[Keys.FORCE_OFFLINE] = next.forceOfflineMode
            prefs[Keys.MEMORY_ENABLED] = next.memoryEnabled
            prefs[Keys.REPLAY_PROTECTION] = next.replayProtectionEnabled
            prefs[Keys.SECURITY_PIN_ENABLED] = next.securityPinEnabled
            prefs[Keys.SECURITY_PIN_CODE] = next.securityPinCode
            prefs[Keys.VOICEPRINT_ENROLLED] = next.voicePrintEnrolled
            prefs[Keys.VOICEPRINT_SIGNATURE] = next.voicePrintSignature
            prefs[Keys.LAST_VERIFIED_SCORE] = next.lastVerifiedScore
            prefs[Keys.HUD_SENSITIVITY_NOTES] = next.hudSensitivityNotes
        }
    }
}
