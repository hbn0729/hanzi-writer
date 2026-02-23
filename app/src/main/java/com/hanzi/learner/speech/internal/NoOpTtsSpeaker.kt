package com.hanzi.learner.speech.internal

import com.hanzi.learner.speech.contract.TtsEngineInfo
import com.hanzi.learner.speech.contract.TtsSpeakerContract
import com.hanzi.learner.speech.contract.TtsVoiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class NoOpTtsSpeaker : TtsSpeakerContract {
    private val _isReady = MutableStateFlow(true)
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

    override fun speak(text: String) {}

    override fun speakCharacterAndPhrase(character: String, phrase: String) {}

    override fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
    }

    override fun setPitch(pitch: Float) {
        _pitch.value = pitch
    }

    override fun stop() {}

    override fun shutdown() {}

    override fun setEngine(enginePackageName: String) {}
}
