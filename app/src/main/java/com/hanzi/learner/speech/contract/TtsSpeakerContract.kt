package com.hanzi.learner.speech.contract

import kotlinx.coroutines.flow.StateFlow

/**
 * High-level TTS speaker contract for UI layer.
 * Bridges synthesis and playback following Dependency Inversion Principle.
 */
interface TtsSpeakerContract {
    val isReady: StateFlow<Boolean>
    fun speak(text: String)
    fun speakCharacterAndPhrase(character: String, phrase: String)
    fun shutdown()
}
