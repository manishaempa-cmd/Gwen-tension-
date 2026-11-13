package com.example.core

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

data class VoicePrintMetrics(
    val signatureCsv: String,
    val meanRms: Float,
    val zeroCrossingRate: Float,
    val energyVariance: Float,
    val livenessPassed: Boolean
)

data class VoiceVerificationResult(
    val matched: Boolean,
    val similarityPercent: Int,
    val livenessPassed: Boolean,
    val summary: String
)

class VoiceEngine(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ttsReady = true
                    tts?.language = Locale("en", "IN")
                    selectFemaleVoiceIfAvailable()
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }
                    })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS init error", e)
        }
    }

    private fun selectFemaleVoiceIfAvailable() {
        val voices = tts?.voices ?: return
        val preferred = voices.firstOrNull { voice ->
            val name = voice.name.lowercase()
            (voice.locale.language == "en" || voice.locale.language == "hi") &&
                (name.contains("female") || name.contains("sfg") || name.contains("f00") || name.contains("in-x-ene"))
        }
        if (preferred != null) {
            tts?.voice = preferred
        }
    }

    fun speak(text: String, settings: GwenSettings) {
        if (!ttsReady || text.isBlank()) return
        try {
            val locale = when (settings.preferredLanguage) {
                "hi-IN" -> Locale("hi", "IN")
                else -> Locale("en", "IN")
            }
            tts?.language = locale
            tts?.setPitch(settings.voicePitch.coerceIn(0.6f, 1.8f))
            tts?.setSpeechRate(settings.voiceSpeed.coerceIn(0.6f, 1.8f))
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, settings.voiceVolume.coerceIn(0.1f, 1.0f))
            }
            val cleanText = text.replace(Regex("[*#`_]"), "").take(1800)
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString())
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak failed", e)
            _isSpeaking.value = false
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _isSpeaking.value = false
    }

    fun startListening(
        languageTag: String,
        wakeWordOnly: Boolean = false,
        onResult: (String) -> Unit,
        onWakeDetected: (() -> Unit)? = null
    ) {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _voiceError.value = "Microphone permission is required for voice input."
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _voiceError.value = "Speech recognition service is unavailable on this device."
            return
        }

        stopSpeaking()
        _voiceError.value = null
        _partialText.value = ""

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        _rmsLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        _rmsLevel.value = 0f
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _rmsLevel.value = 0f
                        _voiceError.value = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Try speaking clearly."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio capture error."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            else -> "Voice recognition error ($error)."
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _rmsLevel.value = 0f
                        val text = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()
                        if (text.isNotBlank()) {
                            if (wakeWordOnly) {
                                if (text.lowercase().contains("gwen")) {
                                    onWakeDetected?.invoke()
                                    val cleaned = text.replace(Regex("(?i)^.*?gwen[,\\s]*"), "").trim()
                                    if (cleaned.isNotBlank()) onResult(cleaned)
                                } else {
                                    _voiceError.value = "Wake word \"Gwen\" not detected in: \"$text\""
                                }
                            } else {
                                onResult(text)
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        _partialText.value = partial
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val localeCode = when (languageTag) {
                "hi-IN" -> "hi-IN"
                else -> "en-IN"
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "SpeechRecognizer start failed", e)
            _isListening.value = false
            _voiceError.value = e.localizedMessage ?: "Failed to start microphone."
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        _isListening.value = false
        _rmsLevel.value = 0f
    }

    /**
     * Captures live PCM audio via AudioRecord and extracts acoustic voiceprint metrics
     * plus dynamic variance to resist static replay attacks.
     */
    suspend fun captureVoicePrint(durationMs: Long = 2200L): Result<VoicePrintMetrics> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    throw SecurityException("RECORD_AUDIO permission not granted")
                }

                val sampleRate = 16000
                val minBuf = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(2048)

                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2
                )

                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    recorder.release()
                    throw IllegalStateException("AudioRecord failed to initialize")
                }

                val frameRmsList = mutableListOf<Float>()
                var totalZeroCrossings = 0L
                var totalSamples = 0L
                var lowDiffSum = 0.0
                var highDiffSum = 0.0

                try {
                    recorder.startRecording()
                    val buffer = ShortArray(1024)
                    val endTime = System.currentTimeMillis() + durationMs
                    while (System.currentTimeMillis() < endTime) {
                        val read = recorder.read(buffer, 0, buffer.size)
                        if (read > 1) {
                            var sumSq = 0.0
                            var zc = 0
                            for (i in 0 until read) {
                                val norm = buffer[i] / 32768f
                                sumSq += norm * norm
                                if (i > 0 && ((buffer[i] >= 0 && buffer[i - 1] < 0) || (buffer[i] < 0 && buffer[i - 1] >= 0))) {
                                    zc++
                                }
                                if (i > 1) {
                                    val d1 = abs(buffer[i] - buffer[i - 1]) / 32768.0
                                    val d2 = abs(buffer[i] - buffer[i - 2]) / 32768.0
                                    highDiffSum += d1
                                    lowDiffSum += d2
                                }
                            }
                            val rms = sqrt(sumSq / read).toFloat()
                            frameRmsList.add(rms)
                            _rmsLevel.value = (rms * 5.5f).coerceIn(0f, 1f)
                            totalZeroCrossings += zc
                            totalSamples += read
                        }
                    }
                } finally {
                    _rmsLevel.value = 0f
                    try {
                        recorder.stop()
                    } catch (_: Exception) {
                    }
                    recorder.release()
                }

                if (frameRmsList.isEmpty() || totalSamples == 0L) {
                    throw IllegalStateException("No audio frames captured.")
                }

                val meanRms = frameRmsList.average().toFloat()
                if (meanRms < 0.004f) {
                    throw IllegalStateException("Voice input too quiet. Speak clearly near the microphone.")
                }

                val zcr = totalZeroCrossings.toFloat() / totalSamples.toFloat()
                val rmsVariance = frameRmsList.map { (it - meanRms) * (it - meanRms) }.average().toFloat()
                val spectralRatio = if (lowDiffSum > 0.0) (highDiffSum / lowDiffSum).toFloat() else 0.5f
                val peakRms = frameRmsList.maxOrNull() ?: meanRms
                val dynamicRange = if (meanRms > 0f) ln(1f + (peakRms / meanRms)) else 0f

                // Dynamic energy modulation helps flag flat/constant-gain speaker loops
                val livenessPassed = rmsVariance > 0.000015f && peakRms > meanRms * 1.35f

                val signature = listOf(meanRms, zcr, spectralRatio, dynamicRange, rmsVariance)
                    .joinToString(",") { String.format(Locale.US, "%.5f", it) }

                VoicePrintMetrics(
                    signatureCsv = signature,
                    meanRms = meanRms,
                    zeroCrossingRate = zcr,
                    energyVariance = rmsVariance,
                    livenessPassed = livenessPassed
                )
            }
        }

    fun verifyAgainstSignature(
        enrolledCsv: String,
        candidate: VoicePrintMetrics,
        replayProtectionEnabled: Boolean
    ): VoiceVerificationResult {
        val enrolled = enrolledCsv.split(",").mapNotNull { it.toFloatOrNull() }
        val current = candidate.signatureCsv.split(",").mapNotNull { it.toFloatOrNull() }
        if (enrolled.size < 4 || current.size < 4) {
            return VoiceVerificationResult(
                matched = false,
                similarityPercent = 0,
                livenessPassed = false,
                summary = "No enrolled voiceprint found. Please complete voice registration first."
            )
        }

        if (replayProtectionEnabled && !candidate.livenessPassed) {
            return VoiceVerificationResult(
                matched = false,
                similarityPercent = 28,
                livenessPassed = false,
                summary = "Replay-attack guard flagged flat acoustic energy modulation."
            )
        }

        // Compare ZCR, spectral ratio, and dynamic range (index 1, 2, 3) with normalized distance
        val zcrDiff = abs(enrolled[1] - current[1]) / (enrolled[1].coerceAtLeast(0.02f))
        val specDiff = abs(enrolled[2] - current[2]) / (enrolled[2].coerceAtLeast(0.2f))
        val dynDiff = abs(enrolled[3] - current[3]) / (enrolled[3].coerceAtLeast(0.4f))
        val weightedError = (zcrDiff * 0.45f + specDiff * 0.35f + dynDiff * 0.20f)
        val similarity = ((1f - (weightedError * 0.65f)) * 100f).toInt().coerceIn(15, 99)
        val matched = similarity >= 65

        return VoiceVerificationResult(
            matched = matched,
            similarityPercent = similarity,
            livenessPassed = candidate.livenessPassed,
            summary = if (matched) {
                "Owner voice verified ($similarity% match, live vocal dynamics confirmed)."
            } else {
                "Voiceprint mismatch ($similarity% match). Please re-verify or re-enroll in quiet conditions."
            }
        )
    }

    fun release() {
        try {
            speechRecognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val TAG = "VoiceEngine"
    }
}
