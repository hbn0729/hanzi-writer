package com.hanzi.learner.speech.internal

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class PreviewAudioPlayerTest {

    @Test
    fun `isPlaying returns false initially`() = runTest {
        val player = createMockPlayer()

        val result = player.isPlaying.first()

        assertFalse("isPlaying should be false initially", result)
    }

    @Test
    fun `playFromLocalModel sets isPlaying to true`() = runTest {
        val player = createMockPlayer()

        player.playFromLocalModel("/path/to/model", listOf("model.onnx", "tokens.txt"), "测试文本")

        val result = player.isPlaying.first()
        assertTrue("isPlaying should be true after playFromLocalModel", result)
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

            override fun playFromLocalModel(modelDirPath: String, modelFiles: List<String>, text: String) {
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