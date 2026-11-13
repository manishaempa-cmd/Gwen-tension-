package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.DeviceController
import com.example.core.GwenSettings
import com.example.core.NetworkMonitor
import com.example.core.PreferencesStore
import com.example.core.VoiceEngine
import com.example.core.VoiceVerificationResult
import com.example.data.local.GwenLocalDatabase
import com.example.data.local.LocalOfflineNote
import com.example.data.model.AiChatMode
import com.example.data.model.AvatarMode
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.GwenExpression
import com.example.data.model.MemoryItem
import com.example.data.repository.GeminiAiRepository
import com.example.data.repository.GwenFirestoreRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AiStreamState {
    data object Idle : AiStreamState
    data object Thinking : AiStreamState
    data class Streaming(val partialText: String) : AiStreamState
    data class Error(val message: String) : AiStreamState
}

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

class GwenAssistantViewModel(
    private val currentUserId: String,
    private val userDisplayName: String,
    private val firestoreRepo: GwenFirestoreRepository,
    private val geminiRepo: GeminiAiRepository,
    private val localDb: GwenLocalDatabase,
    private val preferencesStore: PreferencesStore,
    private val networkMonitor: NetworkMonitor,
    val voiceEngine: VoiceEngine,
    val deviceController: DeviceController
) : ViewModel() {

    val settings: StateFlow<GwenSettings> = preferencesStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), GwenSettings())

    val systemNetworkOnline: StateFlow<Boolean> = networkMonitor.isOnlineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), networkMonitor.isCurrentlyOnline())

    val isEffectiveOnline: StateFlow<Boolean> = combine(systemNetworkOnline, settings) { net, prefs ->
        net && !prefs.forceOfflineMode
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    private val _activeConversationId = MutableStateFlow("conv_default_${currentUserId.take(8)}")
    val activeConversationId: StateFlow<String> = _activeConversationId.asStateFlow()

    private val _chatMode = MutableStateFlow(AiChatMode.GENERAL)
    val chatMode: StateFlow<AiChatMode> = _chatMode.asStateFlow()

    private val _expression = MutableStateFlow(GwenExpression.HAPPY)
    val expression: StateFlow<GwenExpression> = _expression.asStateFlow()

    private val _liveStatus = MutableStateFlow("Ready • Systems Optimal")
    val liveStatus: StateFlow<String> = _liveStatus.asStateFlow()

    private val _aiStreamState = MutableStateFlow<AiStreamState>(AiStreamState.Idle)
    val aiStreamState: StateFlow<AiStreamState> = _aiStreamState.asStateFlow()

    private val _lastLatencyMs = MutableStateFlow<Long?>(null)
    val lastLatencyMs: StateFlow<Long?> = _lastLatencyMs.asStateFlow()

    private val _enrollmentStatus = MutableStateFlow<String?>(null)
    val enrollmentStatus: StateFlow<String?> = _enrollmentStatus.asStateFlow()

    private val _verificationResult = MutableStateFlow<VoiceVerificationResult?>(null)
    val verificationResult: StateFlow<VoiceVerificationResult?> = _verificationResult.asStateFlow()

    val conversationsState: StateFlow<DataUiState<List<Conversation>>> =
        firestoreRepo.observeConversations(currentUserId)
            .map<List<Conversation>, DataUiState<List<Conversation>>> { DataUiState.Success(it) }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                emit(DataUiState.Error((fsError ?: error).message ?: "Failed to load conversations"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messagesState: StateFlow<DataUiState<List<ChatMessage>>> =
        _activeConversationId.flatMapLatest { convId ->
            if (convId.isBlank()) {
                flowOf(DataUiState.Success(emptyList()))
            } else {
                firestoreRepo.observeMessages(currentUserId, convId)
                    .map<List<ChatMessage>, DataUiState<List<ChatMessage>>> { DataUiState.Success(it) }
                    .catch { error ->
                        val fsError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (fsError == null && error is CancellationException) throw error
                        emit(DataUiState.Error((fsError ?: error).message ?: "Failed to load messages"))
                    }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val memoriesState: StateFlow<DataUiState<List<MemoryItem>>> =
        firestoreRepo.observeMemories(currentUserId)
            .map<List<MemoryItem>, DataUiState<List<MemoryItem>>> { list ->
                syncMemoriesToLocalCache(list)
                DataUiState.Success(list)
            }
            .catch { error ->
                val fsError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsError == null && error is CancellationException) throw error
                emit(DataUiState.Error((fsError ?: error).message ?: "Failed to load memories"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val localOfflineNotes: StateFlow<List<LocalOfflineNote>> =
        localDb.offlineNoteDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private var activeChatJob: Job? = null

    init {
        viewModelScope.launch {
            firestoreRepo.ensureUserProfile(displayName = userDisplayName)
        }
        viewModelScope.launch {
            voiceEngine.isSpeaking.collect { speaking ->
                if (speaking) {
                    _expression.value = GwenExpression.SPEAKING
                    _liveStatus.value = "Speaking..."
                } else if (_aiStreamState.value is AiStreamState.Idle && !voiceEngine.isListening.value) {
                    _liveStatus.value = if (isEffectiveOnline.value) {
                        "Online • Gemini Ready"
                    } else {
                        "Offline Mode • Local Core"
                    }
                }
            }
        }
    }

    private fun syncMemoriesToLocalCache(items: List<MemoryItem>) {
        viewModelScope.launch {
            items.forEach { mem ->
                localDb.offlineNoteDao().upsert(
                    LocalOfflineNote(
                        id = mem.id,
                        title = mem.title,
                        content = mem.content,
                        category = mem.category,
                        updatedAtMillis = mem.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun setExpression(expression: GwenExpression) {
        _expression.value = expression
    }

    fun setChatMode(mode: AiChatMode) {
        _chatMode.value = mode
    }

    fun selectConversation(conversationId: String) {
        _activeConversationId.value = conversationId
    }

    fun startNewConversation() {
        val newId = "conv_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        _activeConversationId.value = newId
        _aiStreamState.value = AiStreamState.Idle
        _expression.value = GwenExpression.HAPPY
        _liveStatus.value = "Fresh session ready, Boss!"
    }

    fun startVoiceCommand(wakeWordOnly: Boolean = false) {
        _expression.value = GwenExpression.LISTENING
        _liveStatus.value = if (wakeWordOnly) "Listening for \"Gwen\"..." else "Listening..."
        voiceEngine.startListening(
            languageTag = settings.value.preferredLanguage,
            wakeWordOnly = wakeWordOnly,
            onWakeDetected = {
                _expression.value = GwenExpression.SURPRISED
            },
            onResult = { transcript ->
                sendPrompt(transcript, attachedBitmap = null)
            }
        )
    }

    fun stopVoiceInput() {
        voiceEngine.stopListening()
        _expression.value = GwenExpression.HAPPY
        _liveStatus.value = "Ready"
    }

    fun stopSpeaking() {
        voiceEngine.stopSpeaking()
        _expression.value = GwenExpression.HAPPY
        _liveStatus.value = "Speech stopped"
    }

    fun previewFemaleVoice() {
        _expression.value = GwenExpression.HAPPY
        voiceEngine.speak(
            "Hello Boss! All neural circuits and holographic systems are online and ready for your command.",
            settings.value
        )
    }

    fun sendPrompt(prompt: String, attachedBitmap: Bitmap? = null) {
        val clean = prompt.trim()
        if (clean.isEmpty() && attachedBitmap == null) return

        activeChatJob?.cancel()
        activeChatJob = viewModelScope.launch {
            val convId = _activeConversationId.value
            val currentSettings = settings.value
            val effectivePrompt = clean.ifBlank { "Analyze this image for me, Boss." }

            _expression.value = GwenExpression.THINKING
            _aiStreamState.value = AiStreamState.Thinking
            _liveStatus.value = if (isEffectiveOnline.value) "Gemini Thinking..." else "Running Offline Core..."

            val currentMsgs = (messagesState.value as? DataUiState.Success)?.data.orEmpty()
            val activeMemories = if (currentSettings.memoryEnabled) {
                (memoriesState.value as? DataUiState.Success)?.data.orEmpty()
            } else {
                emptyList()
            }

            firestoreRepo.appendMessage(
                conversationId = convId,
                role = "user",
                text = effectivePrompt,
                expression = GwenExpression.LISTENING.label,
                hasImage = attachedBitmap != null,
                mode = _chatMode.value.key
            )

            val startTime = System.currentTimeMillis()
            if (!isEffectiveOnline.value) {
                val offlineReply = geminiRepo.processOfflineCommand(
                    query = effectivePrompt,
                    localNotes = localOfflineNotes.value,
                    deviceController = deviceController
                )
                _lastLatencyMs.value = System.currentTimeMillis() - startTime
                _aiStreamState.value = AiStreamState.Idle
                _expression.value = GwenExpression.SERIOUS
                firestoreRepo.appendMessage(
                    conversationId = convId,
                    role = "assistant",
                    text = offlineReply,
                    expression = GwenExpression.SERIOUS.label,
                    hasImage = false,
                    mode = _chatMode.value.key
                )
                if (currentSettings.autoSpeakResponses) {
                    voiceEngine.speak(offlineReply, currentSettings)
                }
                return@launch
            }

            try {
                var finalResponse = ""
                geminiRepo.streamChat(
                    modelName = currentSettings.preferredModel,
                    mode = _chatMode.value,
                    languageTag = currentSettings.preferredLanguage,
                    history = currentMsgs,
                    memories = activeMemories,
                    userPrompt = effectivePrompt,
                    attachedBitmap = attachedBitmap
                ).collect { accumulated ->
                    finalResponse = accumulated
                    _aiStreamState.value = AiStreamState.Streaming(accumulated)
                    _expression.value = GwenExpression.SPEAKING
                    _liveStatus.value = "Streaming Response..."
                }

                _lastLatencyMs.value = System.currentTimeMillis() - startTime
                _aiStreamState.value = AiStreamState.Idle
                val chosenExpression = detectExpressionFromText(finalResponse)
                _expression.value = chosenExpression
                _liveStatus.value = "Online • Verified"

                firestoreRepo.appendMessage(
                    conversationId = convId,
                    role = "assistant",
                    text = finalResponse,
                    expression = chosenExpression.label,
                    hasImage = false,
                    mode = _chatMode.value.key
                )

                if (currentSettings.autoSpeakResponses) {
                    voiceEngine.speak(finalResponse, currentSettings)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat generation error", e)
                val msg = e.localizedMessage ?: "Failed to generate Gemini response."
                _aiStreamState.value = AiStreamState.Error(msg)
                _expression.value = GwenExpression.WORRIED
                _liveStatus.value = "AI Error"
            }
        }
    }

    private fun detectExpressionFromText(text: String): GwenExpression {
        val lower = text.lowercase()
        return when {
            lower.contains("haha") || lower.contains("funny") || lower.contains("joke") -> GwenExpression.LAUGHING
            lower.contains("careful") || lower.contains("warning") || lower.contains("error") -> GwenExpression.WORRIED
            lower.contains("whoa") || lower.contains("amazing") || lower.contains("surpris") -> GwenExpression.SURPRISED
            lower.contains("confused") || lower.contains("unclear") -> GwenExpression.CONFUSED
            lower.contains("important") || lower.contains("security") || lower.contains("critical") -> GwenExpression.SERIOUS
            else -> GwenExpression.HAPPY
        }
    }

    // --- MEMORY ACTIONS ---

    fun saveMemory(title: String, content: String, category: String) {
        viewModelScope.launch {
            if (!settings.value.memoryEnabled) return@launch
            val localId = "local_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            localDb.offlineNoteDao().upsert(
                LocalOfflineNote(
                    id = localId,
                    title = title,
                    content = content,
                    category = category,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
            if (isEffectiveOnline.value) {
                firestoreRepo.createMemory(title = title, content = content, category = category)
            }
        }
    }

    fun updateMemory(memoryId: String, title: String, content: String, category: String) {
        viewModelScope.launch {
            localDb.offlineNoteDao().upsert(
                LocalOfflineNote(
                    id = memoryId,
                    title = title,
                    content = content,
                    category = category,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
            if (isEffectiveOnline.value) {
                firestoreRepo.updateMemory(memoryId, title, content, category)
            }
        }
    }

    fun deleteMemory(memoryId: String) {
        viewModelScope.launch {
            localDb.offlineNoteDao().deleteById(memoryId)
            if (isEffectiveOnline.value) {
                firestoreRepo.deleteMemory(memoryId)
            }
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            localDb.offlineNoteDao().clearAll()
            if (isEffectiveOnline.value) {
                firestoreRepo.clearAllMemories()
            }
        }
    }

    // --- VOICE REGISTRATION & VERIFICATION ---

    fun enrollOwnerVoicePrint() {
        viewModelScope.launch {
            _enrollmentStatus.value = "Recording voiceprint sample (speak: \"Gwen, recognize my voice\")..."
            _expression.value = GwenExpression.LISTENING
            val result = voiceEngine.captureVoicePrint(2400L)
            result.onSuccess { metrics ->
                preferencesStore.update {
                    it.copy(
                        voicePrintEnrolled = true,
                        voicePrintSignature = metrics.signatureCsv
                    )
                }
                if (isEffectiveOnline.value) {
                    firestoreRepo.updateUserProfileSettings(voiceEnrolled = true)
                }
                _expression.value = GwenExpression.HAPPY
                _enrollmentStatus.value = "Voiceprint enrolled! ZCR=${"%.3f".format(metrics.zeroCrossingRate)}, Liveness=${if (metrics.livenessPassed) "Live" else "Low variance"}."
            }.onFailure { err ->
                _expression.value = GwenExpression.WORRIED
                _enrollmentStatus.value = err.localizedMessage ?: "Voice enrollment failed."
            }
        }
    }

    fun verifyOwnerVoicePrint() {
        viewModelScope.launch {
            _verificationResult.value = null
            _expression.value = GwenExpression.LISTENING
            val capture = voiceEngine.captureVoicePrint(2200L)
            capture.onSuccess { metrics ->
                val currentSettings = settings.value
                val res = voiceEngine.verifyAgainstSignature(
                    enrolledCsv = currentSettings.voicePrintSignature,
                    candidate = metrics,
                    replayProtectionEnabled = currentSettings.replayProtectionEnabled
                )
                _verificationResult.value = res
                _expression.value = if (res.matched) GwenExpression.HAPPY else GwenExpression.ANGRY
                if (res.matched) {
                    preferencesStore.update { it.copy(lastVerifiedScore = res.similarityPercent) }
                }
            }.onFailure { err ->
                _expression.value = GwenExpression.WORRIED
                _verificationResult.value = VoiceVerificationResult(
                    matched = false,
                    similarityPercent = 0,
                    livenessPassed = false,
                    summary = err.localizedMessage ?: "Microphone capture failed."
                )
            }
        }
    }

    // --- SETTINGS UPDATES ---

    fun updateSettings(transform: (GwenSettings) -> GwenSettings) {
        viewModelScope.launch {
            preferencesStore.update(transform)
            val latest = preferencesStore.settingsFlow.first()
            if (isEffectiveOnline.value) {
                firestoreRepo.updateUserProfileSettings(
                    preferredLanguage = latest.preferredLanguage,
                    preferredModel = latest.preferredModel,
                    memoryEnabled = latest.memoryEnabled,
                    voiceEnrolled = latest.voicePrintEnrolled
                )
            }
        }
    }

    fun setAvatarMode(mode: AvatarMode) {
        viewModelScope.launch {
            preferencesStore.update { it.copy(avatarMode = mode) }
        }
    }

    fun toggleWakeService(enabled: Boolean) {
        viewModelScope.launch {
            preferencesStore.update { it.copy(backgroundListeningEnabled = enabled, wakeWordEnabled = enabled) }
            deviceController.setWakeServiceRunning(enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.release()
    }

    companion object {
        private const val TAG = "GwenAssistantVM"

        fun provideFactory(
            currentUserId: String,
            userDisplayName: String,
            firestoreRepo: GwenFirestoreRepository,
            geminiRepo: GeminiAiRepository,
            localDb: GwenLocalDatabase,
            preferencesStore: PreferencesStore,
            networkMonitor: NetworkMonitor,
            voiceEngine: VoiceEngine,
            deviceController: DeviceController
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GwenAssistantViewModel(
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName,
                    firestoreRepo = firestoreRepo,
                    geminiRepo = geminiRepo,
                    localDb = localDb,
                    preferencesStore = preferencesStore,
                    networkMonitor = networkMonitor,
                    voiceEngine = voiceEngine,
                    deviceController = deviceController
                )
            }
        }
    }
}
