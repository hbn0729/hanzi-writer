package com.hanzi.learner.speech.model

data class TtsSettings(
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
) {
    companion object {
        val DEFAULT = TtsSettings()
        val MIN_SPEECH_RATE = 0.5f
        val MAX_SPEECH_RATE = 2.0f
        val MIN_PITCH = 0.5f
        val MAX_PITCH = 2.0f
    }
}
