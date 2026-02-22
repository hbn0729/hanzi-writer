package com.hanzi.learner.speech.internal

import com.hanzi.learner.speech.contract.TtsSpeakerContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class NoOpTtsSpeaker : TtsSpeakerContract {
    private val _isReady = MutableStateFlow(true)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    override fun speak(text: String) {}

    override fun speakCharacterAndPhrase(character: String, phrase: String) {}

    override fun shutdown() {}
}

