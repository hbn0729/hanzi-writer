package com.hanzi.learner.speech.internal

import org.junit.Assert.assertTrue
import org.junit.Test

class NoOpTtsSpeakerTest {
    @Test
    fun `NoOpTtsSpeaker is always ready and does nothing`() {
        val speaker = NoOpTtsSpeaker()
        assertTrue(speaker.isReady.value)
        speaker.speak("hello")
        speaker.speakCharacterAndPhrase("汉", "汉字")
        speaker.shutdown()
    }
}

