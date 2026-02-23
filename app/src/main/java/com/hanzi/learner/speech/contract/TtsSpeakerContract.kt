package com.hanzi.learner.speech.contract

import kotlinx.coroutines.flow.StateFlow

interface TtsSpeakerContract {
    val isReady: StateFlow<Boolean>

    fun speak(text: String)
    fun speakCharacterAndPhrase(character: String, phrase: String)
    fun stop()
    fun shutdown()
}
