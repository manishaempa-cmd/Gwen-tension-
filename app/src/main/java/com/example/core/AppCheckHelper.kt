package com.example.core

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

@Volatile
private var lastIntentDebugTokenPresent = false

data class AppCheckDiagnosticResult(
    val providerName: String,
    val isDebugBuild: Boolean,
    val hasIntentDebugToken: Boolean,
    val debugSecretUuid: String?,
    val tokenValid: Boolean,
    val statusMessage: String
)

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        if (BuildConfig.DEBUG) {
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)

            if (!debugToken.isNullOrEmpty()) {
                lastIntentDebugTokenPresent = true
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully from Intent.")
            } else {
                Log.w(
                    TAG,
                    "No FIREBASE_APPCHECK_DEBUG_TOKEN in Intent extra. " +
                        "On physical devices, ensure the device's Debug Secret UUID is registered in Firebase Console -> App Check."
                )
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            val resId = context.resources.getIdentifier(
                RECAPTCHA_SITE_KEY_RES,
                "string",
                context.packageName
            )
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

fun getStoredDebugSecret(context: Context): String? {
    if (!BuildConfig.DEBUG) return null
    return try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        val persistenceKey = FirebaseApp.getInstance().persistenceKey
        val prefs = context.getSharedPreferences(
            "$DEBUG_STORE_PREFIX$persistenceKey",
            Context.MODE_PRIVATE
        )
        prefs.getString(DEBUG_SECRET_KEY, null)
    } catch (e: Exception) {
        Log.w(TAG, "Unable to read App Check debug secret", e)
        null
    }
}

fun getAppCheckProviderName(context: Context): String {
    if (BuildConfig.DEBUG) return "DebugAppCheckProviderFactory"
    val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
    val siteKey = if (resId != 0) context.getString(resId) else ""
    return if (siteKey.isNotBlank()) {
        "RecaptchaAppCheckProviderFactory"
    } else {
        "Missing recaptcha_site_key"
    }
}

suspend fun verifyAppCheckSeparately(
    context: Context,
    forceRefresh: Boolean = true
): AppCheckDiagnosticResult {
    val provider = getAppCheckProviderName(context)
    return try {
        val tokenResult = FirebaseAppCheck.getInstance().getAppCheckToken(forceRefresh).await()
        val secretAfterCall = getStoredDebugSecret(context)
        val expiresAt = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            .format(Date(tokenResult.expireTimeMillis))
        val validToken = tokenResult.token.isNotBlank()
        AppCheckDiagnosticResult(
            providerName = provider,
            isDebugBuild = BuildConfig.DEBUG,
            hasIntentDebugToken = lastIntentDebugTokenPresent,
            debugSecretUuid = secretAfterCall,
            tokenValid = validToken,
            statusMessage = if (validToken) {
                "Verified • Token active (expires $expiresAt)"
            } else {
                "Failed • Blank App Check token returned"
            }
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val secretAfterCall = getStoredDebugSecret(context)
        val rawMsg = e.localizedMessage ?: "App Check attestation failed"
        Log.e(TAG, "Separate App Check verification failed: $rawMsg", e)
        val hint = if (BuildConfig.DEBUG && !lastIntentDebugTokenPresent && !secretAfterCall.isNullOrBlank()) {
            " • Register Debug Token in Firebase Console -> App Check"
        } else {
            ""
        }
        AppCheckDiagnosticResult(
            providerName = provider,
            isDebugBuild = BuildConfig.DEBUG,
            hasIntentDebugToken = lastIntentDebugTokenPresent,
            debugSecretUuid = secretAfterCall,
            tokenValid = false,
            statusMessage = "Invalid: $rawMsg$hint"
        )
    }
}
