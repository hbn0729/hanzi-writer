package com.hanzi.learner.speech.internal

import android.content.Context
import android.util.Log
import com.hanzi.learner.speech.SpeechModule
import com.hanzi.learner.speech.contract.TtsSpeakerContract
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

/**
 * TTS speaker that attempts system TTS first, falls back to built-in TTS if unavailable.
 */
internal class FallbackTtsSpeaker(
    context: Context,
    private val config: SpeechModule.TtsConfig = SpeechModule.TtsConfig(),
) : TtsSpeakerContract {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var activeSpeaker: TtsSpeakerContract? = null
    private var systemTtsSpeaker: SystemTtsSpeaker? = null
    private var builtInSpeaker: SherpaOnnxTtsSpeaker? = null

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private data class PendingRequest(
        val type: RequestType,
        val text: String = "",
        val character: String = "",
        val phrase: String = "",
    )

    private enum class RequestType {
        SPEAK,
        SPEAK_CHARACTER_AND_PHRASE,
    }

    private var pendingRequest: PendingRequest? = null

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
                    processPendingRequest()
                }
            }
        }
    }

    private fun processPendingRequest() {
        pendingRequest?.let { pending ->
            Log.d(TAG, "Processing pending request")
            pendingRequest = null
            when (pending.type) {
                RequestType.SPEAK -> speak(pending.text)
                RequestType.SPEAK_CHARACTER_AND_PHRASE -> speakCharacterAndPhrase(
                    pending.character,
                    pending.phrase
                )
            }
        }
    }

    override fun speak(text: String) {
        val speaker = activeSpeaker
        if (speaker == null || !_isReady.value) {
            Log.w(TAG, "Speaker not ready, queuing speak request")
            pendingRequest = PendingRequest(RequestType.SPEAK, text = text)
            return
        }
        speaker.speak(text)
    }

    override fun speakCharacterAndPhrase(character: String, phrase: String) {
        val speaker = activeSpeaker
        if (speaker == null || !_isReady.value) {
            Log.w(TAG, "Speaker not ready, queuing speakCharacterAndPhrase request")
            pendingRequest = PendingRequest(
                RequestType.SPEAK_CHARACTER_AND_PHRASE,
                character = character,
                phrase = phrase
            )
            return
        }
        speaker.speakCharacterAndPhrase(character, phrase)
    }

    override fun shutdown() {
        Log.d(TAG, "Shutting down FallbackTtsSpeaker")
        systemTtsSpeaker?.shutdown()
        builtInSpeaker?.shutdown()
        activeSpeaker = null
        _isReady.value = false
    }
}
