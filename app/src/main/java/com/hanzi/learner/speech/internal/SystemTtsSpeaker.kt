package com.hanzi.learner.speech.internal

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.hanzi.learner.speech.contract.TtsSpeakerContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "SystemTtsSpeaker"

/**
 * TTS speaker using Android system TTS engine.
 * Falls back gracefully if system TTS is unavailable or doesn't support Chinese.
 */
internal class SystemTtsSpeaker(
    context: Context,
) : TtsSpeakerContract {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var tts: TextToSpeech? = null
    private var isEngineReady = false
    private var isChineseSupported = false

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
        initializeTts()
    }

    private fun initializeTts() {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d(TAG, "System TTS engine initialized")
                isEngineReady = true
                checkChineseSupport()
            } else {
                Log.e(TAG, "System TTS initialization failed with status: $status")
                _isReady.value = false
            }
        }
    }

    private fun checkChineseSupport() {
        val ttsInstance = tts ?: return

        val chineseLocale = Locale.CHINESE
        val result = ttsInstance.isLanguageAvailable(chineseLocale)

        isChineseSupported = result >= TextToSpeech.LANG_AVAILABLE
        Log.d(TAG, "Chinese support check: $result (LANG_AVAILABLE=${TextToSpeech.LANG_AVAILABLE}), supported=$isChineseSupported")

        if (isChineseSupported) {
            val setLocaleResult = ttsInstance.setLanguage(chineseLocale)
            Log.d(TAG, "Set Chinese locale result: $setLocaleResult")
        }

        ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "Utterance started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "Utterance done: $utteranceId")
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Utterance error: $utteranceId")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "Utterance error: $utteranceId, code: $errorCode")
            }
        })

        _isReady.value = isEngineReady && isChineseSupported

        if (_isReady.value) {
            pendingRequest?.let { pending ->
                Log.d(TAG, "Processing pending request after initialization")
                pendingRequest = null
                when (pending.type) {
                    RequestType.SPEAK -> executeSpeak(pending.text)
                    RequestType.SPEAK_CHARACTER_AND_PHRASE -> executeSpeakCharacterAndPhrase(
                        pending.character,
                        pending.phrase
                    )
                }
            }
        }
    }

    override fun speak(text: String) {
        Log.d(TAG, "speak() called: $text, isReady: ${_isReady.value}")
        if (!_isReady.value) {
            Log.w(TAG, "Speaker not ready, queuing speak request")
            pendingRequest = PendingRequest(RequestType.SPEAK, text = text)
            return
        }
        executeSpeak(text)
    }

    private fun executeSpeak(text: String) {
        val ttsInstance = tts
        if (ttsInstance == null || !isEngineReady) {
            Log.e(TAG, "TTS not available for speak")
            return
        }

        ttsInstance.stop()
        ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speak_${System.currentTimeMillis()}")
    }

    override fun speakCharacterAndPhrase(character: String, phrase: String) {
        Log.d(TAG, "speakCharacterAndPhrase() called: '$character', '$phrase', isReady: ${_isReady.value}")
        if (!_isReady.value) {
            Log.w(TAG, "Speaker not ready, queuing speakCharacterAndPhrase request")
            pendingRequest = PendingRequest(
                RequestType.SPEAK_CHARACTER_AND_PHRASE,
                character = character,
                phrase = phrase
            )
            return
        }
        executeSpeakCharacterAndPhrase(character, phrase)
    }

    private fun executeSpeakCharacterAndPhrase(character: String, phrase: String) {
        val ttsInstance = tts
        if (ttsInstance == null || !isEngineReady) {
            Log.e(TAG, "TTS not available for speakCharacterAndPhrase")
            return
        }

        scope.launch {
            ttsInstance.stop()

            val charUtteranceId = "char_${System.currentTimeMillis()}"
            ttsInstance.speak(character, TextToSpeech.QUEUE_FLUSH, null, charUtteranceId)

            if (phrase.isNotEmpty()) {
                delay(800)
                val phraseUtteranceId = "phrase_${System.currentTimeMillis()}"
                ttsInstance.speak(phrase, TextToSpeech.QUEUE_ADD, null, phraseUtteranceId)
            }
        }
    }

    override fun shutdown() {
        Log.d(TAG, "Shutting down System TTS")
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
    }
}
