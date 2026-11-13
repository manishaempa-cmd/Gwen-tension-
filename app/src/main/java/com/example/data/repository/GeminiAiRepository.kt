package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.core.DeviceController
import com.example.core.GwenAccessibilityService
import com.example.data.local.LocalOfflineNote
import com.example.data.model.AiChatMode
import com.example.data.model.ChatMessage
import com.example.data.model.MemoryItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StructuredTaskPlan(
    val category: String,
    val planSummary: String,
    val requiresInternet: Boolean,
    val actionType: String,
    val actionTarget: String
)

data class ScreenAnalysisResult(
    val summary: String,
    val ocrText: String,
    val uiElements: List<String>,
    val suggestedNextStep: String
)

class GeminiAiRepository {

    private val imageGenModel by lazy {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config
        )
    }

    private val taskPlannerModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "category" to Schema.enumeration(listOf("phone", "ai", "media", "screen", "memory")),
                "planSummary" to Schema.string(description = "Concise step-by-step execution plan"),
                "requiresInternet" to Schema.boolean(description = "True if internet is needed"),
                "actionType" to Schema.enumeration(
                    listOf("launch_app", "youtube_search", "volume", "scroll_down", "scroll_up", "read_screen", "memory_save", "ai_answer")
                ),
                "actionTarget" to Schema.string(description = "Target app name, search query, or parameter")
            )
        )
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = schema
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    private val screenVisionModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "summary" to Schema.string(description = "Clear explanation of the screen"),
                "ocrText" to Schema.string(description = "Extracted visible text from the screenshot"),
                "uiElements" to Schema.array(
                    items = Schema.string(),
                    description = "Detected buttons, inputs, tabs, or interactive elements"
                ),
                "suggestedNextStep" to Schema.string(description = "Recommended helpful next action")
            )
        )
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = schema
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    fun streamChat(
        modelName: String,
        mode: AiChatMode,
        languageTag: String,
        history: List<ChatMessage>,
        memories: List<MemoryItem>,
        userPrompt: String,
        attachedBitmap: Bitmap? = null
    ): Flow<String> = flow {
        val memoryContext = if (memories.isNotEmpty()) {
            "\n\nUser Personal Memory Context:\n" +
                memories.take(8).joinToString("\n") { "- ${it.title}: ${it.content}" }
        } else {
            ""
        }
        val languageHint = when (languageTag) {
            "hi-IN" -> "\nRespond naturally in Hindi (Devanagari or clear Hindi) unless asked otherwise."
            "hinglish" -> "\nRespond naturally in conversational Hinglish (blend of Hindi and English in Latin script)."
            else -> "\nRespond in clear, intelligent English."
        }
        val systemPrompt = mode.systemInstruction + languageHint + memoryContext

        val safeModelName = modelName.takeIf {
            it in setOf("gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-4-argon")
        } ?: "gemini-3.8-flash"

        val model = Firebase.ai.generativeModel(
            modelName = safeModelName,
            systemInstruction = content { text(systemPrompt) }
        )

        val chatHistory = history.takeLast(10).map { msg ->
            val role = if (msg.role == "user") "user" else "model"
            content(role) { text(msg.text) }
        }

        val chat = model.startChat(history = chatHistory)
        val promptContent = content("user") {
            if (attachedBitmap != null) {
                image(attachedBitmap)
            }
            text(userPrompt)
        }

        val sb = StringBuilder()
        try {
            withTimeout(60_000L) {
                chat.sendMessageStream(promptContent).collect { chunk ->
                    val delta = chunk.text.orEmpty()
                    if (delta.isNotEmpty()) {
                        sb.append(delta)
                        emit(sb.toString())
                    }
                }
            }
            if (sb.isBlank()) {
                throw IllegalStateException("Gemini returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini streaming timed out", e)
            throw IllegalStateException("Gemini AI timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini streaming failed", e)
            throw mapAiException(e)
        }
    }

    suspend fun testGeminiConnection(modelName: String): Result<String> {
        return try {
            val safeModelName = modelName.takeIf {
                it in setOf("gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-4-argon")
            } ?: "gemini-3.8-flash"
            val model = Firebase.ai.generativeModel(modelName = safeModelName)
            val response = withTimeout(30_000L) {
                model.generateContent("Reply with the single word: ONLINE")
            }
            val text = response.text?.trim().orEmpty()
            if (text.isNotBlank()) {
                Result.success("Connected ($safeModelName): $text")
            } else {
                Result.failure(IllegalStateException("Gemini returned an empty ping response."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini connection test failed", e)
            Result.failure(mapAiException(e))
        }
    }

    suspend fun planTaskWithGemini(command: String): StructuredTaskPlan {
        return try {
            val response = withTimeout(45_000L) {
                taskPlannerModel.generateContent(
                    "Classify and plan this Android personal assistant command for Gwen AI: \"$command\""
                )
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty task plan response")
            val json = JSONObject(jsonText)
            StructuredTaskPlan(
                category = json.optString("category", "ai"),
                planSummary = json.optString("planSummary", "Analyze and respond to user request."),
                requiresInternet = json.optBoolean("requiresInternet", true),
                actionType = json.optString("actionType", "ai_answer"),
                actionTarget = json.optString("actionTarget", "")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Structured task planning failed", e)
            throw mapAiException(e)
        }
    }

    suspend fun analyzeScreenshot(bitmap: Bitmap, focusPrompt: String): ScreenAnalysisResult {
        return try {
            val response = withTimeout(60_000L) {
                screenVisionModel.generateContent(
                    content {
                        image(bitmap)
                        text(
                            focusPrompt.ifBlank {
                                "Analyze this Android screen: extract OCR text, list interactive buttons/UI elements, explain what is shown, and recommend the next step."
                            }
                        )
                    }
                )
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty vision response")
            val json = JSONObject(jsonText)
            val elementsArray = json.optJSONArray("uiElements")
            val elements = if (elementsArray != null) {
                (0 until elementsArray.length()).mapNotNull { idx ->
                    elementsArray.optString(idx).takeIf { it.isNotBlank() }
                }
            } else {
                emptyList()
            }
            ScreenAnalysisResult(
                summary = json.optString("summary", "Screen analyzed."),
                ocrText = json.optString("ocrText", ""),
                uiElements = elements,
                suggestedNextStep = json.optString("suggestedNextStep", "")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Screen understanding failed", e)
            throw mapAiException(e)
        }
    }

    suspend fun generateImage(prompt: String): Bitmap {
        return try {
            val response = withTimeout(60_000L) {
                imageGenModel.generateContent(prompt)
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts
                ?.filterIsInstance<com.google.firebase.ai.type.ImagePart>()
                ?.firstOrNull()
            imagePart?.image ?: throw IllegalStateException("No image was returned by Gemini Nano Banana 2.1.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            throw IllegalStateException("Image generation timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            throw mapAiException(e)
        }
    }

    suspend fun editImage(inputBitmap: Bitmap, instruction: String): Bitmap {
        return try {
            val response = withTimeout(60_000L) {
                imageGenModel.generateContent(
                    content {
                        image(inputBitmap)
                        text(instruction)
                    }
                )
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts
                ?.filterIsInstance<com.google.firebase.ai.type.ImagePart>()
                ?.firstOrNull()
            imagePart?.image ?: throw IllegalStateException("No edited image was returned by the model.")
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image editing timed out", e)
            throw IllegalStateException("Image editing timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image editing failed", e)
            throw mapAiException(e)
        }
    }

    /**
     * Lightweight deterministic offline command & local memory engine when internet is unavailable
     * or Offline Mode is forced by the user.
     */
    suspend fun processOfflineCommand(
        query: String,
        localNotes: List<LocalOfflineNote>,
        deviceController: DeviceController
    ): String {
        val lower = query.trim().lowercase()
        return when {
            lower.startsWith("open ") || lower.startsWith("launch ") -> {
                val appName = lower.removePrefix("open ").removePrefix("launch ").trim()
                val launched = deviceController.findAndLaunchAppByName(appName)
                if (launched != null) {
                    "Offline Action Verified: Launched ${launched.label} (${launched.packageName})."
                } else {
                    "Offline Command: Could not find an installed app matching \"$appName\"."
                }
            }
            lower.contains("time") -> {
                val time = SimpleDateFormat("hh:mm a, EEE dd MMM", Locale.getDefault()).format(Date())
                "Offline Clock: It is currently $time, Boss."
            }
            lower.contains("battery") || lower.contains("ram") || lower.contains("status") -> {
                val t = deviceController.getDeviceTelemetry()
                "Offline Telemetry (${t.deviceModel}): Battery ${t.batteryPercent}% (${if (t.isCharging) "Charging" else "Discharging"}, ${t.batteryTempC}°C) • RAM Free ${t.ramAvailableMb}MB / ${t.ramTotalMb}MB."
            }
            lower.contains("scroll down") -> {
                val ok = GwenAccessibilityService.instance?.performScroll(forward = true) ?: false
                if (ok) "Offline Accessibility Verified: Scrolled down." else "Accessibility service is not enabled in Android Settings."
            }
            lower.contains("scroll up") -> {
                val ok = GwenAccessibilityService.instance?.performScroll(forward = false) ?: false
                if (ok) "Offline Accessibility Verified: Scrolled up." else "Accessibility service is not enabled in Android Settings."
            }
            lower.contains("youtube") -> {
                val q = lower.replace("search youtube for", "").replace("youtube", "").trim()
                deviceController.openYouTubeSearch(q)
                "Offline Intent: Opening YouTube${if (q.isNotBlank()) " for \"$q\"" else ""} (YouTube playback requires internet)."
            }
            lower.contains("note") || lower.contains("memory") || lower.contains("remember") -> {
                val hits = localNotes.filter {
                    it.title.lowercase().contains(lower) || it.content.lowercase().contains(lower)
                }
                if (hits.isNotEmpty()) {
                    "Offline Local Memory Match:\n" + hits.take(3).joinToString("\n") { "• ${it.title}: ${it.content}" }
                } else {
                    "Offline Mode Active: ${localNotes.size} local cached memories available. Add notes in Memory Center or go Online for Gemini AI reasoning."
                }
            }
            else -> {
                val matchingNotes = localNotes.filter { note ->
                    lower.split(" ").any { word ->
                        word.length > 3 && (note.title.lowercase().contains(word) || note.content.lowercase().contains(word))
                    }
                }
                if (matchingNotes.isNotEmpty()) {
                    "Offline Local Match (${matchingNotes.first().title}): ${matchingNotes.first().content}"
                } else {
                    "Offline Mode Active: I can launch installed apps, adjust volume/brightness, check battery/RAM, and search ${localNotes.size} local notes. Cloud Gemini AI reasoning requires an internet connection."
                }
            }
        }
    }

    private fun mapAiException(e: Exception): Exception {
        val raw = e.localizedMessage ?: e.message ?: "Unknown AI error"
        val isAppCheck = raw.contains("App Check", ignoreCase = true) ||
            raw.contains("AppCheck", ignoreCase = true) ||
            raw.contains("attestation", ignoreCase = true)
        return if (isAppCheck) {
            IllegalStateException(
                "$raw — Open AI Status Dashboard to verify App Check and register your device Debug Token in Firebase Console if running a Debug APK on a physical phone.",
                e
            )
        } else {
            e
        }
    }

    private companion object {
        const val TAG = "GeminiAiRepo"
    }
}
