package com.hanzi.learner.speech.internal

import android.content.Context
import android.util.Log
import com.hanzi.learner.speech.SpeechModule
import com.hanzi.learner.speech.contract.TtsEngineInfo
import com.hanzi.learner.speech.contract.TtsSpeakerContract
import com.hanzi.learner.speech.contract.TtsVoiceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "FallbackTtsSpeaker"

private const val SYSTEM_TTS_INIT_TIMEOUT_MS = 3000L

internal class FallbackTtsSpeaker(
    context: Context,
    private val config: SpeechModule.TtsConfig = SpeechModule.TtsConfig(),
) : TtsSpeakerContract {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val pendingRequestHandler = PendingRequestHandler()

    private var activeSpeaker: TtsSpeakerContract? = null
    private var systemTtsSpeaker: SystemTtsSpeaker? = null
    private var builtInSpeaker: SherpaOnnxTtsSpeaker? = null

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    override val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _pitch = MutableStateFlow(1.0f)
    override val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _currentEngine = MutableStateFlow<TtsEngineInfo?>(null)
    override val currentEngine: StateFlow<TtsEngineInfo?> = _currentEngine.asStateFlow()

    private val _availableEngines = MutableStateFlow<List<TtsEngineInfo>>(emptyList())
    override val availableEngines: StateFlow<List<TtsEngineInfo>> = _availableEngines.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceInfo>>(emptyList())
    override val availableVoices: StateFlow<List<TtsVoiceInfo>> = _availableVoices.asStateFlow()

    private val _isChineseSupported = MutableStateFlow(false)
    override val isChineseSupported: StateFlow<Boolean> = _isChineseSupported.asStateFlow()

    init {
        attemptSystemTts()
    }

    private fun attemptSystemTts() {
        Log.d(TAG, "Attempting system TTS...")
        systemTtsSpeaker = SystemTtsSpeaker(appContext)

        scope.launch {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < SYSTEM_TTS_INIT_TIMEOUT_MS) {
                if (systemTtsSpeaker?.isReady?.value == true) {
                    Log.d(TAG, "System TTS is ready, using it")
                    activeSpeaker = systemTtsSpeaker
                    _isReady.value = true
                    syncSpeakerState()
                    processPendingRequest()
                    return@launch
                }
                delay(100)
            }

            Log.w(TAG, "System TTS not ready after timeout, falling back to built-in TTS")
            systemTtsSpeaker?.shutdown()
            systemTtsSpeaker = null

            initializeBuiltInTts()
        }
    }

    private fun initializeBuiltInTts() {
        Log.d(TAG, "Initializing built-in TTS...")
        builtInSpeaker = SpeechModule.createTtsSpeaker(appContext, config) as? SherpaOnnxTtsSpeaker
        activeSpeaker = builtInSpeaker

        scope.launch {
            builtInSpeaker?.isReady?.collect { ready ->
                _isReady.value = ready
                if (ready) {
                    syncSpeakerState()
                    processPendingRequest()
                }
            }
        }
    }

    private fun syncSpeakerState() {
        activeSpeaker?.let { speaker ->
            _speechRate.value = speaker.speechRate.value
            _pitch.value = speaker.pitch.value
            _currentEngine.value = speaker.currentEngine.value
            _availableEngines.value = speaker.availableEngines.value
            _availableVoices.value = speaker.availableVoices.value
            _isChineseSupported.value = speaker.isChineseSupported.value
        }
    }

    private fun processPendingRequest() {
        Log.d(TAG, "Processing pending request")
        pendingRequestHandler.processIfReady(_isReady.value) { request ->
            when (request) {
                is TtsRequest.Speak -> speak(request.text)
                is TtsRequest.SpeakCharacterAndPhrase -> speakCharacterAndPhrase(
                    request.character,
                    request.phrase
                )
            }
        }
    }

    override fun speak(text: String) {
        val speaker = activeSpeaker
        if (speaker == null || !_isReady.value) {
            Log.w(TAG, "Speaker not ready, queuing speak request")
            pendingRequestHandler.enqueue(TtsRequest.Speak(text))
            return
        }
        speaker.speak(text)
    }

    override fun speakCharacterAndPhrase(character: String, phrase: String) {
        val speaker = activeSpeaker
        if (speaker == null || !_isReady.value) {
            Log.w(TAG, "Speaker not ready, queuing speakCharacterAndPhrase request")
            pendingRequestHandler.enqueue(TtsRequest.SpeakCharacterAndPhrase(character, phrase))
            return
        }
        speaker.speakCharacterAndPhrase(character, phrase)
    }

    override fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        activeSpeaker?.setSpeechRate(rate)
    }

    override fun setPitch(pitch: Float) {
        _pitch.value = pitch
        activeSpeaker?.setPitch(pitch)
    }

    override fun stop() {
        activeSpeaker?.stop()
    }

    override fun shutdown() {
        Log.d(TAG, "Shutting down FallbackTtsSpeaker")
        systemTtsSpeaker?.shutdown()
        builtInSpeaker?.shutdown()
        activeSpeaker = null
        _isReady.value = false
    }
}
