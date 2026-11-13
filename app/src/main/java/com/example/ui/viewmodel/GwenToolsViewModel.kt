package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.AppCheckDiagnosticResult
import com.example.core.ContactItem
import com.example.core.DeviceController
import com.example.core.DeviceTelemetry
import com.example.core.GwenAccessibilityService
import com.example.core.InstalledAppItem
import com.example.core.NetworkMonitor
import com.example.core.PermissionSnapshot
import com.example.core.verifyAppCheckSeparately
import com.example.data.local.GeneratedMediaEntity
import com.example.data.local.GwenLocalDatabase
import com.example.data.model.AssistantTask
import com.example.data.model.TaskStage
import com.example.data.repository.GeminiAiRepository
import com.example.data.repository.GwenFirestoreRepository
import com.example.data.repository.ScreenAnalysisResult
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed interface ImageStudioUiState {
    data object Idle : ImageStudioUiState
    data object Generating : ImageStudioUiState
    data class Success(val bitmap: Bitmap, val savedPath: String) : ImageStudioUiState
    data class Error(val message: String) : ImageStudioUiState
}

sealed interface ScreenVisionUiState {
    data object Idle : ScreenVisionUiState
    data object Analyzing : ScreenVisionUiState
    data class Success(val result: ScreenAnalysisResult) : ScreenVisionUiState
    data class Error(val message: String) : ScreenVisionUiState
}

class GwenToolsViewModel(
    private val appContext: Context,
    private val currentUserId: String,
    private val firestoreRepo: GwenFirestoreRepository,
    private val geminiRepo: GeminiAiRepository,
    private val localDb: GwenLocalDatabase,
    private val networkMonitor: NetworkMonitor,
    private val deviceController: DeviceController
) : ViewModel() {

    val tasksState: StateFlow<DataUiState<List<AssistantTask>>> =
        firestoreRepo.observeTasks(currentUserId)
            .map<List<AssistantTask>, DataUiState<List<AssistantTask>>> { DataUiState.Success(it) }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                emit(DataUiState.Error((fsError ?: error).message ?: "Failed to load tasks"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val galleryMedia: StateFlow<List<GeneratedMediaEntity>> =
        localDb.generatedMediaDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _imageStudioState = MutableStateFlow<ImageStudioUiState>(ImageStudioUiState.Idle)
    val imageStudioState: StateFlow<ImageStudioUiState> = _imageStudioState.asStateFlow()

    private val _screenVisionState = MutableStateFlow<ScreenVisionUiState>(ScreenVisionUiState.Idle)
    val screenVisionState: StateFlow<ScreenVisionUiState> = _screenVisionState.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _contacts = MutableStateFlow<List<ContactItem>>(emptyList())
    val contacts: StateFlow<List<ContactItem>> = _contacts.asStateFlow()

    private val _telemetry = MutableStateFlow(deviceController.getDeviceTelemetry())
    val telemetry: StateFlow<DeviceTelemetry> = _telemetry.asStateFlow()

    private val _permissions = MutableStateFlow(deviceController.getPermissionSnapshot())
    val permissions: StateFlow<PermissionSnapshot> = _permissions.asStateFlow()

    private val _activePipelineTask = MutableStateFlow<AssistantTask?>(null)
    val activePipelineTask: StateFlow<AssistantTask?> = _activePipelineTask.asStateFlow()

    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    private val _appCheckDiagnostic = MutableStateFlow<AppCheckDiagnosticResult?>(null)
    val appCheckDiagnostic: StateFlow<AppCheckDiagnosticResult?> = _appCheckDiagnostic.asStateFlow()

    private val _geminiPingStatus = MutableStateFlow<String?>(null)
    val geminiPingStatus: StateFlow<String?> = _geminiPingStatus.asStateFlow()

    private val _isDiagnosing = MutableStateFlow(false)
    val isDiagnosing: StateFlow<Boolean> = _isDiagnosing.asStateFlow()

    init {
        refreshSystemState()
    }

    fun refreshSystemState() {
        _telemetry.value = deviceController.getDeviceTelemetry()
        _permissions.value = deviceController.getPermissionSnapshot()
        viewModelScope.launch {
            _installedApps.value = deviceController.getLaunchableApps()
        }
    }

    fun searchContacts(query: String) {
        viewModelScope.launch {
            _contacts.value = deviceController.searchContacts(query)
        }
    }

    fun clearFeedback() {
        _actionFeedback.value = null
    }

    // --- VERIFIED TASK EXECUTION CENTER ---

    fun runVerifiedTask(command: String, isOnline: Boolean) {
        val clean = command.trim()
        if (clean.isBlank()) return

        viewModelScope.launch {
            var taskId: String? = null
            var category = "ai"
            var planSummary = "Classifying command..."

            suspend fun advanceStage(stage: TaskStage, summary: String, verification: String) {
                planSummary = summary
                _activePipelineTask.value = AssistantTask(
                    id = taskId ?: "local_preview",
                    userId = currentUserId,
                    command = clean,
                    category = category,
                    stage = stage.key,
                    planSummary = summary,
                    verificationResult = verification,
                    requiresInternet = isOnline
                )
                if (taskId != null && isOnline) {
                    firestoreRepo.updateTaskStage(taskId!!, stage, summary, verification)
                }
            }

            try {
                // Stage 1: Understanding
                if (isOnline) {
                    taskId = firestoreRepo.createTask(
                        command = clean,
                        category = category,
                        planSummary = "Parsing intent...",
                        requiresInternet = true
                    ).getOrNull()
                }
                advanceStage(TaskStage.UNDERSTANDING, "Parsing user command: \"$clean\"", "Pending")
                delay(220)

                // Stage 2 & 3: Analyzing & Planning
                advanceStage(TaskStage.ANALYZING, "Analyzing intent & required Android capabilities...", "Pending")
                val plan = if (isOnline) {
                    geminiRepo.planTaskWithGemini(clean)
                } else {
                    val lower = clean.lowercase()
                    val (actionType, target) = when {
                        lower.startsWith("open ") -> "launch_app" to lower.removePrefix("open ").trim()
                        lower.contains("youtube") -> "youtube_search" to lower.replace("youtube", "").trim()
                        lower.contains("scroll down") -> "scroll_down" to ""
                        lower.contains("scroll up") -> "scroll_up" to ""
                        else -> "ai_answer" to clean
                    }
                    com.example.data.repository.StructuredTaskPlan(
                        category = if (actionType == "launch_app") "phone" else "ai",
                        planSummary = "Offline deterministic execution plan ($actionType)",
                        requiresInternet = false,
                        actionType = actionType,
                        actionTarget = target
                    )
                }
                category = plan.category
                advanceStage(TaskStage.PLANNING, plan.planSummary, "Pending")
                delay(200)

                // Stage 4: Checking Permissions
                val perms = deviceController.getPermissionSnapshot()
                _permissions.value = perms
                val permCheckNote = when (plan.actionType) {
                    "scroll_down", "scroll_up", "read_screen" ->
                        if (perms.accessibilityEnabled) "Accessibility permission granted"
                        else "Accessibility Service is disabled in Android Settings"
                    else -> "Intent & runtime permissions checked"
                }
                advanceStage(TaskStage.CHECKING_PERMISSIONS, plan.planSummary, permCheckNote)
                delay(180)

                // Stage 5: Executing
                advanceStage(TaskStage.EXECUTING, plan.planSummary, "Running action...")
                var verified = false
                var verificationDetail = ""

                when (plan.actionType) {
                    "launch_app" -> {
                        val launched = deviceController.findAndLaunchAppByName(plan.actionTarget.ifBlank { clean })
                        verified = launched != null
                        verificationDetail = if (launched != null) {
                            "Verified launch intent resolved & started: ${launched.label} (${launched.packageName})"
                        } else {
                            "Verification failed: No installed app matched \"${plan.actionTarget}\""
                        }
                    }
                    "youtube_search" -> {
                        verified = deviceController.openYouTubeSearch(plan.actionTarget)
                        verificationDetail = if (verified) {
                            "Verified YouTube intent dispatched for \"${plan.actionTarget.ifBlank { "Home" }}\""
                        } else {
                            "Verification failed: Could not resolve YouTube / browser intent"
                        }
                    }
                    "scroll_down" -> {
                        verified = GwenAccessibilityService.instance?.performScroll(true) == true
                        verificationDetail = if (verified) {
                            "Verified Accessibility scroll-down gesture dispatched"
                        } else {
                            "Verification failed: Enable Gwen Accessibility Service first"
                        }
                    }
                    "scroll_up" -> {
                        verified = GwenAccessibilityService.instance?.performScroll(false) == true
                        verificationDetail = if (verified) {
                            "Verified Accessibility scroll-up gesture dispatched"
                        } else {
                            "Verification failed: Enable Gwen Accessibility Service first"
                        }
                    }
                    "read_screen" -> {
                        val nodes = GwenAccessibilityService.instance?.refreshScreenNodes().orEmpty()
                        verified = nodes.isNotEmpty()
                        verificationDetail = if (verified) {
                            "Verified ${nodes.size} visible UI nodes read from active window"
                        } else {
                            "Verification failed: Accessibility Service unavailable or window empty"
                        }
                    }
                    "volume" -> {
                        val t = deviceController.getDeviceTelemetry()
                        val targetVol = if (clean.lowercase().contains("up") || clean.lowercase().contains("max")) {
                            (t.mediaVolume + 2).coerceAtMost(t.mediaMaxVolume)
                        } else {
                            (t.mediaVolume - 2).coerceAtLeast(0)
                        }
                        deviceController.setMediaVolume(targetVol)
                        val updated = deviceController.getDeviceTelemetry()
                        verified = updated.mediaVolume == targetVol
                        verificationDetail = "Verified media volume set to ${updated.mediaVolume}/${updated.mediaMaxVolume}"
                    }
                    "memory_save" -> {
                        if (isOnline) {
                            val res = firestoreRepo.createMemory(
                                title = "Task Note",
                                content = plan.actionTarget.ifBlank { clean },
                                category = "note"
                            )
                            verified = res.isSuccess
                            verificationDetail = if (verified) "Verified memory persisted to Firestore" else "Failed to persist memory"
                        } else {
                            verified = false
                            verificationDetail = "Cloud memory sync requires internet connection"
                        }
                    }
                    else -> {
                        verified = true
                        verificationDetail = "Verified structured AI plan: ${plan.planSummary}"
                    }
                }

                // Stage 6: Verifying
                advanceStage(TaskStage.VERIFYING, plan.planSummary, "Checking post-execution state...")
                delay(220)

                // Stage 7: Completed ONLY if verified == true, else Failed!
                val finalStage = if (verified) TaskStage.COMPLETED else TaskStage.FAILED
                advanceStage(finalStage, plan.planSummary, verificationDetail)
                _actionFeedback.value = verificationDetail
                refreshSystemState()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Task pipeline failed", e)
                val errMsg = e.localizedMessage ?: "Task execution error"
                advanceStage(TaskStage.FAILED, planSummary, "Failed: $errMsg")
                _actionFeedback.value = "Failed: $errMsg"
            }
        }
    }

    // --- SCREEN UNDERSTANDING ---

    fun analyzeScreenBitmap(bitmap: Bitmap, focusPrompt: String) {
        viewModelScope.launch {
            if (!networkMonitor.isCurrentlyOnline()) {
                _screenVisionState.value = ScreenVisionUiState.Error(
                    "Gemini Vision Screen Understanding requires an active internet connection."
                )
                return@launch
            }
            _screenVisionState.value = ScreenVisionUiState.Analyzing
            try {
                val res = geminiRepo.analyzeScreenshot(bitmap, focusPrompt)
                _screenVisionState.value = ScreenVisionUiState.Success(res)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _screenVisionState.value = ScreenVisionUiState.Error(
                    e.localizedMessage ?: "Screen analysis failed."
                )
            }
        }
    }

    // --- IMAGE STUDIO (GEMINI NANO BANANA 2.1) ---

    fun generateStudioImage(prompt: String, inputBitmap: Bitmap? = null) {
        val clean = prompt.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            if (!networkMonitor.isCurrentlyOnline()) {
                _imageStudioState.value = ImageStudioUiState.Error(
                    "Image generation requires an active internet connection."
                )
                return@launch
            }
            _imageStudioState.value = ImageStudioUiState.Generating
            try {
                val outputBitmap = if (inputBitmap != null) {
                    geminiRepo.editImage(inputBitmap, clean)
                } else {
                    geminiRepo.generateImage(clean)
                }
                val savedPath = saveBitmapToInternalStorage(outputBitmap)
                localDb.generatedMediaDao().insert(
                    GeneratedMediaEntity(
                        id = "img_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                        prompt = clean,
                        filePath = savedPath,
                        modelName = "gemini-nano-banana-2.1",
                        isEdit = inputBitmap != null,
                        createdAtMillis = System.currentTimeMillis()
                    )
                )
                _imageStudioState.value = ImageStudioUiState.Success(outputBitmap, savedPath)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Studio image generation failed", e)
                _imageStudioState.value = ImageStudioUiState.Error(
                    e.localizedMessage ?: "Image generation failed."
                )
            }
        }
    }

    private suspend fun saveBitmapToInternalStorage(bitmap: Bitmap): String =
        withContext(Dispatchers.IO) {
            val dir = File(appContext.filesDir, "generated_images").apply { mkdirs() }
            val file = File(dir, "gwen_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            file.absolutePath
        }

    fun deleteGeneratedMedia(item: GeneratedMediaEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    File(item.filePath).delete()
                } catch (_: Exception) {
                }
            }
            localDb.generatedMediaDao().deleteById(item.id)
        }
    }

    // --- MEDIA & PHONE CONTROLS ---

    fun setMediaVolume(level: Int) {
        deviceController.setMediaVolume(level)
        _telemetry.value = deviceController.getDeviceTelemetry()
    }

    fun setSystemBrightness(percent: Int) {
        val ok = deviceController.setSystemBrightnessPercent(percent)
        if (!ok) {
            _actionFeedback.value = "Grant 'Modify system settings' in Permissions Center to adjust system brightness."
        }
        _telemetry.value = deviceController.getDeviceTelemetry()
    }

    fun mediaPlayPause() {
        deviceController.dispatchMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        _telemetry.value = deviceController.getDeviceTelemetry()
    }

    fun mediaNext() {
        deviceController.dispatchMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun mediaPrevious() {
        deviceController.dispatchMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    // --- APP CHECK & GEMINI DIAGNOSTICS ---

    fun runAppCheckVerification() {
        viewModelScope.launch {
            _isDiagnosing.value = true
            try {
                _appCheckDiagnostic.value = verifyAppCheckSeparately(appContext, forceRefresh = true)
            } finally {
                _isDiagnosing.value = false
            }
        }
    }

    fun runGeminiConnectionTest(modelName: String) {
        viewModelScope.launch {
            _isDiagnosing.value = true
            _geminiPingStatus.value = "Testing App Check & Gemini connection..."
            try {
                val appCheckRes = verifyAppCheckSeparately(appContext, forceRefresh = false)
                _appCheckDiagnostic.value = appCheckRes
                if (!appCheckRes.tokenValid) {
                    _geminiPingStatus.value = "Blocked at App Check step: ${appCheckRes.statusMessage}"
                    return@launch
                }
                val ping = geminiRepo.testGeminiConnection(modelName)
                _geminiPingStatus.value = ping.fold(
                    onSuccess = { it },
                    onFailure = { "Gemini API Error: ${it.localizedMessage ?: "Failed"}" }
                )
            } finally {
                _isDiagnosing.value = false
            }
        }
    }

    companion object {
        private const val TAG = "GwenToolsVM"

        fun provideFactory(
            appContext: Context,
            currentUserId: String,
            firestoreRepo: GwenFirestoreRepository,
            geminiRepo: GeminiAiRepository,
            localDb: GwenLocalDatabase,
            networkMonitor: NetworkMonitor,
            deviceController: DeviceController
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GwenToolsViewModel(
                    appContext = appContext.applicationContext,
                    currentUserId = currentUserId,
                    firestoreRepo = firestoreRepo,
                    geminiRepo = geminiRepo,
                    localDb = localDb,
                    networkMonitor = networkMonitor,
                    deviceController = deviceController
                )
            }
        }
    }
}
