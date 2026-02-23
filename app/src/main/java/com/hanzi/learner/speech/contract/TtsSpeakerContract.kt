package com.hanzi.learner.speech.contract

import kotlinx.coroutines.flow.StateFlow

data class TtsEngineInfo(
    val name: String,
    val packageName: String,
    val isDefault: Boolean = false,
)

data class TtsVoiceInfo(
    val name: String,
    val locale: String,
    val quality: Int = 0,
)

interface TtsSpeaker {
    val isReady: StateFlow<Boolean>

    fun speak(text: String)
    fun speakCharacterAndPhrase(character: String, phrase: String)
    fun stop()
    fun shutdown()
}

interface TtsSettingsControl {
    val speechRate: StateFlow<Float>
    val pitch: StateFlow<Float>

    fun setSpeechRate(rate: Float)
    fun setPitch(pitch: Float)
}

interface TtsEngineInfoProvider {
    val currentEngine: StateFlow<TtsEngineInfo?>
    val availableEngines: StateFlow<List<TtsEngineInfo>>
    val availableVoices: StateFlow<List<TtsVoiceInfo>>
    val isChineseSupported: StateFlow<Boolean>
}

interface TtsSpeakerContract : TtsSpeaker, TtsSettingsControl, TtsEngineInfoProvider
