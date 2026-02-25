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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "SystemTtsSpeaker"

internal class SystemTtsSpeaker(
    context: Context,
) : TtsSpeakerContract {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val pendingRequestHandler = PendingRequestHandler()

    private var tts: TextToSpeech? = null
    private var isEngineReady = false
    private var isReinitializing = false

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        initializeTts()
    }

    private fun initializeTts() {
        _isReady.value = false
        isEngineReady = false

        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d(TAG, "System TTS engine initialized with default engine")
                isEngineReady = true
                isReinitializing = false
                configureChineseLocale()
            } else {
                Log.e(TAG, "System TTS initialization failed with status: $status")
                _isReady.value = false
                isReinitializing = false
            }
        }
    }

    private fun reinitializeTts() {
        if (isReinitializing) {
            Log.d(TAG, "Reinitialization already in progress, skipping")
            return
        }
        Log.d(TAG, "Reinitializing TTS engine due to disconnection")
        isReinitializing = true
        _isReady.value = false
        isEngineReady = false

        tts?.stop()
        tts?.shutdown()
        tts = null

        scope.launch {
            delay(100)
            initializeTts()
        }
    }

    private fun configureChineseLocale() {
        val ttsInstance = tts ?: return

        val chineseLocales = listOf(
            Locale.CHINESE,
            Locale.SIMPLIFIED_CHINESE,
            Locale("zh", "TW"),
        )

        var bestResult = TextToSpeech.LANG_NOT_SUPPORTED
        var bestLocale = Locale.CHINESE

        for (locale in chineseLocales) {
            val result = ttsInstance.isLanguageAvailable(locale)
            Log.d(TAG, "Chinese support check for $locale: $result")
            if (result > bestResult) {
                bestResult = result
                bestLocale = locale
            }
            if (result >= TextToSpeech.LANG_AVAILABLE) break
        }

        val chineseSupported = bestResult >= TextToSpeech.LANG_AVAILABLE
        Log.d(TAG, "Chinese support: supported=$chineseSupported, bestLocale=$bestLocale")

        if (chineseSupported) {
            val setLocaleResult = ttsInstance.setLanguage(bestLocale)
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

        _isReady.value = isEngineReady

        if (_isReady.value) {
            processPendingRequest()
        }
    }

    override fun speak(text: String) {
        Log.d(TAG, "speak() called: $text, isReady: ${_isReady.value}")
        if (!_isReady.value || isReinitializing) {
            Log.w(TAG, "Speaker not ready, queuing speak request")
            pendingRequestHandler.enqueue(TtsRequest.Speak(text))
            return
        }
        executeSpeak(text)
    }

    private fun executeSpeak(text: String) {
        val ttsInstance = tts
        if (ttsInstance == null || !isEngineReady) {
            Log.e(TAG, "TTS not available for speak, triggering reinitialization")
            reinitializeTts()
            pendingRequestHandler.enqueue(TtsRequest.Speak(text))
            return
        }

        val stopResult = ttsInstance.stop()
        if (stopResult == TextToSpeech.ERROR) {
            Log.e(TAG, "TTS stop failed, engine may be disconnected, triggering reinitialization")
            reinitializeTts()
            pendingRequestHandler.enqueue(TtsRequest.Speak(text))
            return
        }

        val speakResult = ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speak_${System.currentTimeMillis()}")
        if (speakResult == TextToSpeech.ERROR) {
            Log.e(TAG, "TTS speak failed, engine may be disconnected, triggering reinitialization")
            reinitializeTts()
            pendingRequestHandler.enqueue(TtsRequest.Speak(text))
        }
    }

    override fun speakCharacterAndPhrase(character: String, phrase: String) {
        Log.d(TAG, "speakCharacterAndPhrase() called: '$character', '$phrase', isReady: ${_isReady.value}")
        if (!_isReady.value || isReinitializing) {
            Log.w(TAG, "Speaker not ready, queuing speakCharacterAndPhrase request")
            pendingRequestHandler.enqueue(TtsRequest.SpeakCharacterAndPhrase(character, phrase))
            return
        }
        executeSpeakCharacterAndPhrase(character, phrase)
    }

    private fun executeSpeakCharacterAndPhrase(character: String, phrase: String) {
        val ttsInstance = tts
        if (ttsInstance == null || !isEngineReady) {
            Log.e(TAG, "TTS not available for speakCharacterAndPhrase, triggering reinitialization")
            reinitializeTts()
            pendingRequestHandler.enqueue(TtsRequest.SpeakCharacterAndPhrase(character, phrase))
            return
        }

        val stopResult = ttsInstance.stop()
        if (stopResult == TextToSpeech.ERROR) {
            Log.e(TAG, "TTS stop failed, engine may be disconnected, triggering reinitialization")
            reinitializeTts()
            pendingRequestHandler.enqueue(TtsRequest.SpeakCharacterAndPhrase(character, phrase))
            return
        }

        scope.launch {
            val charUtteranceId = "char_${System.currentTimeMillis()}"
            val speakResult = ttsInstance.speak(character, TextToSpeech.QUEUE_FLUSH, null, charUtteranceId)
            if (speakResult == TextToSpeech.ERROR) {
                Log.e(TAG, "TTS speak failed for character, engine may be disconnected, triggering reinitialization")
                reinitializeTts()
                pendingRequestHandler.enqueue(TtsRequest.SpeakCharacterAndPhrase(character, phrase))
                return@launch
            }

            if (phrase.isNotEmpty()) {
                delay(800)
                val phraseUtteranceId = "phrase_${System.currentTimeMillis()}"
                val phraseResult = ttsInstance.speak(phrase, TextToSpeech.QUEUE_ADD, null, phraseUtteranceId)
                if (phraseResult == TextToSpeech.ERROR) {
                    Log.e(TAG, "TTS speak failed for phrase, engine may be disconnected")
                }
            }
        }
    }

    private fun processPendingRequest() {
        Log.d(TAG, "Processing pending request after initialization")
        pendingRequestHandler.processIfReady(_isReady.value) { request ->
            when (request) {
                is TtsRequest.Speak -> executeSpeak(request.text)
                is TtsRequest.SpeakCharacterAndPhrase -> executeSpeakCharacterAndPhrase(
                    request.character,
                    request.phrase
                )
            }
        }
    }

    override fun stop() {
        tts?.stop()
        Log.d(TAG, "TTS stopped")
    }

    override fun shutdown() {
        Log.d(TAG, "Shutting down System TTS")
        scope.cancel() // PWR-07: Cancel any in-flight coroutines
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
    }
}
