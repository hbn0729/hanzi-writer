package com.hanzi.learner.speech.internal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
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
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

private const val TAG = "SystemTtsSpeaker"

internal class SystemTtsSpeaker(
    context: Context,
) : TtsSpeakerContract {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val pendingRequestHandler = PendingRequestHandler()

    private var tts: TextToSpeech? = null
    private var isEngineReady = false
    private var chineseSupported = false

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
        initializeTts()
    }

    private fun initializeTts(engineName: String? = null) {
        _isReady.value = false
        isEngineReady = false
        
        tts = if (engineName != null) {
            TextToSpeech(appContext, { status ->
                if (status == TextToSpeech.SUCCESS) {
                    Log.d(TAG, "System TTS engine initialized: $engineName")
                    isEngineReady = true
                    checkChineseSupport()
                    updateEngineInfo()
                } else {
                    Log.e(TAG, "System TTS initialization failed with status: $status")
                    _isReady.value = false
                }
            }, engineName)
        } else {
            TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    Log.d(TAG, "System TTS engine initialized with default engine")
                    isEngineReady = true
                    checkChineseSupport()
                    updateEngineInfo()
                } else {
                    Log.e(TAG, "System TTS initialization failed with status: $status")
                    _isReady.value = false
                }
            }
        }
    }

    private fun checkChineseSupport() {
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

        chineseSupported = bestResult >= TextToSpeech.LANG_AVAILABLE
        _isChineseSupported.value = chineseSupported
        Log.d(TAG, "Chinese support result: supported=$chineseSupported, bestResult=$bestResult, bestLocale=$bestLocale")

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

    private fun updateEngineInfo() {
        val ttsInstance = tts ?: return

        val enginesFromTts = ttsInstance.engines.map { engine ->
            engine.name to engine.name
        }.toMap()

        val enginesFromPackageManager = queryEnginesViaPackageManager()

        val mergedEngines = mutableMapOf<String, TtsEngineInfo>()

        enginesFromPackageManager.forEach { (packageName, label) ->
            val isDefault = packageName == ttsInstance.defaultEngine
            mergedEngines[packageName] = TtsEngineInfo(
                name = packageName,
                packageName = packageName,
                label = label,
                isDefault = isDefault,
                isChineseSupported = false
            )
        }

        enginesFromTts.forEach { (packageName, _) ->
            if (!mergedEngines.containsKey(packageName)) {
                val label = getEngineLabel(packageName)
                val isDefault = packageName == ttsInstance.defaultEngine
                mergedEngines[packageName] = TtsEngineInfo(
                    name = packageName,
                    packageName = packageName,
                    label = label,
                    isDefault = isDefault,
                    isChineseSupported = false
                )
            }
        }

        val enginesList = mergedEngines.values.toList()
        _availableEngines.value = enginesList

        val defaultEngine = ttsInstance.defaultEngine
        val currentEngineInfo = enginesList.find { it.packageName == defaultEngine }
            ?: enginesList.firstOrNull()
        
        if (currentEngineInfo != null) {
            _currentEngine.value = currentEngineInfo.copy(isChineseSupported = chineseSupported)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val voices = ttsInstance.voices?.map { voice ->
                TtsVoiceInfo(
                    name = voice.name,
                    locale = voice.locale.toString(),
                    quality = voice.quality
                )
            } ?: emptyList()
            _availableVoices.value = voices
        }

        scope.launch {
            checkAllEnginesChineseSupport(enginesList)
        }
    }

    private fun queryEnginesViaPackageManager(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
            val resolveInfos = appContext.packageManager
                .queryIntentServices(intent, PackageManager.MATCH_ALL)

            for (resolveInfo in resolveInfos) {
                val packageName = resolveInfo.serviceInfo.packageName
                val label = getEngineLabel(packageName)
                result[packageName] = label
                Log.d(TAG, "Found TTS engine via PackageManager: $packageName ($label)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying TTS engines via PackageManager", e)
        }
        return result
    }

    private fun getEngineLabel(packageName: String): String {
        return try {
            val appInfo = appContext.packageManager
                .getApplicationInfo(packageName, 0)
            appContext.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            Log.w(TAG, "Could not get label for engine: $packageName", e)
            packageName
        }
    }

    private suspend fun checkAllEnginesChineseSupport(engines: List<TtsEngineInfo>) {
        val updatedEngines = engines.map { engine ->
            val supported = checkEngineChineseSupport(engine.packageName)
            engine.copy(isChineseSupported = supported)
        }
        _availableEngines.value = updatedEngines

        val current = _currentEngine.value
        if (current != null) {
            val updatedCurrent = updatedEngines.find { it.packageName == current.packageName }
            if (updatedCurrent != null) {
                _currentEngine.value = updatedCurrent
            }
        }
    }

    private suspend fun checkEngineChineseSupport(enginePackageName: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            var tempTts: TextToSpeech? = null
            tempTts = TextToSpeech(appContext, { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tempTts?.isLanguageAvailable(Locale.CHINESE) 
                        ?: TextToSpeech.LANG_NOT_SUPPORTED
                    Log.d(TAG, "Chinese support for $enginePackageName: $result")
                    tempTts?.shutdown()
                    if (continuation.isActive) {
                        continuation.resume(result >= TextToSpeech.LANG_AVAILABLE)
                    }
                } else {
                    Log.w(TAG, "Failed to initialize engine $enginePackageName for Chinese check")
                    tempTts?.shutdown()
                    if (continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }, enginePackageName)
            
            continuation.invokeOnCancellation {
                tempTts?.shutdown()
            }
        }
    }

    override fun setEngine(enginePackageName: String) {
        Log.d(TAG, "Switching to engine: $enginePackageName")
        
        tts?.stop()
        tts?.shutdown()
        tts = null
        isEngineReady = false
        _isReady.value = false

        initializeTts(enginePackageName)
    }

    override fun speak(text: String) {
        Log.d(TAG, "speak() called: $text, isReady: ${_isReady.value}")
        if (!_isReady.value) {
            Log.w(TAG, "Speaker not ready, queuing speak request")
            pendingRequestHandler.enqueue(TtsRequest.Speak(text))
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
            pendingRequestHandler.enqueue(TtsRequest.SpeakCharacterAndPhrase(character, phrase))
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

    override fun setSpeechRate(rate: Float) {
        val ttsInstance = tts ?: return
        val clampedRate = rate.coerceIn(0.5f, 2.0f)
        ttsInstance.setSpeechRate(clampedRate)
        _speechRate.value = clampedRate
        Log.d(TAG, "Speech rate set to: $clampedRate")
    }

    override fun setPitch(pitch: Float) {
        val ttsInstance = tts ?: return
        val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
        ttsInstance.setPitch(clampedPitch)
        _pitch.value = clampedPitch
        Log.d(TAG, "Pitch set to: $clampedPitch")
    }

    override fun stop() {
        tts?.stop()
        Log.d(TAG, "TTS stopped")
    }

    override fun shutdown() {
        Log.d(TAG, "Shutting down System TTS")
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
    }
}
