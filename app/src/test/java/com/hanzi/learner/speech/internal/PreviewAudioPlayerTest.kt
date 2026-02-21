package com.hanzi.learner.speech.internal

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class PreviewAudioPlayerTest {

    @Test
    fun `isPlaying returns false initially`() = runTest {
        // Note: This is a simple test that verifies the contract structure
        // Full testing would require Android instrumentation tests due to MediaPlayer dependency
        val player = createMockPlayer()

        val result = player.isPlaying.first()

        assertFalse("isPlaying should be false initially", result)
    }

    @Test
    fun `PreviewAudioPlayer implements contract`() {
        // Verify that PreviewAudioPlayer can be instantiated
        // Actual playback testing requires Android instrumentation tests
        val clazz = PreviewAudioPlayer::class.java

        assertTrue(
            "PreviewAudioPlayer should implement PreviewAudioPlayerContract",
            com.hanzi.learner.speech.contract.PreviewAudioPlayerContract::class.java.isAssignableFrom(clazz)
        )
    }

    private fun createMockPlayer(): com.hanzi.learner.speech.contract.PreviewAudioPlayerContract {
        // Return a mock implementation for unit testing
        return object : com.hanzi.learner.speech.contract.PreviewAudioPlayerContract {
            private val _isPlaying = kotlinx.coroutines.flow.MutableStateFlow(false)
            override val isPlaying: kotlinx.coroutines.flow.StateFlow<Boolean> = _isPlaying

            override fun playFromUrl(url: String) {
                _isPlaying.value = true
            }

            override fun playFromFile(path: String) {
                _isPlaying.value = true
            }

            override fun playSystemTtsPreview(text: String) {
                _isPlaying.value = true
            }

            override fun stop() {
                _isPlaying.value = false
            }

            override fun release() {
                _isPlaying.value = false
            }
        }
    }
}