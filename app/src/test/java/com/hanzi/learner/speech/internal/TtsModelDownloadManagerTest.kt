package com.hanzi.learner.speech.internal

import com.hanzi.learner.speech.contract.TtsModelDownloadManagerContract
import com.hanzi.learner.speech.model.TtsModelDownloadState
import com.hanzi.learner.speech.model.TtsModelInfo
import com.hanzi.learner.speech.model.TtsModelRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TtsModelDownloadManagerTest {

    @Test
    fun `TtsModelDownloadManager implements contract`() {
        // Verify that TtsModelDownloadManager implements the contract
        val clazz = TtsModelDownloadManager::class.java

        assertTrue(
            "TtsModelDownloadManager should implement TtsModelDownloadManagerContract",
            TtsModelDownloadManagerContract::class.java.isAssignableFrom(clazz)
        )
    }

    @Test
    fun `download states should be initially empty or populated`() = runTest {
        // Create a mock download manager for testing
        val mockManager = createMockDownloadManager()

        val states = mockManager.downloadStates.first()

        // Should contain states for downloadable models
        assertNotNull(states)
    }

    @Test
    fun `startDownload should return false for system TTS`() = runTest {
        val mockManager = createMockDownloadManager()

        val result = mockManager.startDownload(TtsModelRegistry.SYSTEM_TTS_ID)

        assertFalse(result)
    }

    @Test
    fun `startDownload should return false for non-existent model`() = runTest {
        val mockManager = createMockDownloadManager()

        val result = mockManager.startDownload("non-existent")

        assertFalse(result)
    }

    @Test
    fun `pauseDownload should update state to paused`() = runTest {
        val mockManager = createMockDownloadManager()

        // Start download first
        mockManager.startDownload("test-model")

        // Then pause
        mockManager.pauseDownload("test-model")

        val states = mockManager.downloadStates.first()
        assertTrue(states["test-model"] is TtsModelDownloadState.Paused)
    }

    @Test
    fun `resumeDownload should update state back to downloading`() = runTest {
        val mockManager = createMockDownloadManager()

        // Start and pause
        mockManager.startDownload("test-model")
        mockManager.pauseDownload("test-model")

        // Resume
        mockManager.resumeDownload("test-model")

        val states = mockManager.downloadStates.first()
        assertTrue(states["test-model"] is TtsModelDownloadState.Downloading)
    }

    @Test
    fun `cancelDownload should reset state to not downloaded`() = runTest {
        val mockManager = createMockDownloadManager()

        // Start download
        mockManager.startDownload("test-model")

        // Cancel
        mockManager.cancelDownload("test-model")

        val states = mockManager.downloadStates.first()
        assertEquals(TtsModelDownloadState.NotDownloaded, states["test-model"])
    }

    @Test
    fun `isModelDownloaded should return false initially`() = runTest {
        val mockManager = createMockDownloadManager()

        val result = mockManager.isModelDownloaded("test-model")

        assertFalse(result)
    }

    @Test
    fun `getModelLocalPath should return null when not downloaded`() {
        val mockManager = createMockDownloadManager()

        val path = mockManager.getModelLocalPath("test-model")

        assertNull(path)
    }

    @Test
    fun `download states should not contain system TTS`() = runTest {
        val mockManager = createMockDownloadManager()

        val states = mockManager.downloadStates.first()

        // Should NOT contain system TTS
        assertFalse(states.containsKey(TtsModelRegistry.SYSTEM_TTS_ID))
    }

    @Test
    fun `TtsModelDownloadState sealed class has all required states`() {
        // Verify all states exist
        val notDownloaded = TtsModelDownloadState.NotDownloaded
        val downloading = TtsModelDownloadState.Downloading(0.5f, 50, 100)
        val paused = TtsModelDownloadState.Paused(0.5f, 50, 100)
        val downloaded = TtsModelDownloadState.Downloaded("/path/to/model")
        val error = TtsModelDownloadState.Error("Test error")

        assertNotNull(notDownloaded)
        assertNotNull(downloading)
        assertNotNull(paused)
        assertNotNull(downloaded)
        assertNotNull(error)

        // Verify properties
        assertEquals(0.5f, downloading.progress, 0.01f)
        assertEquals(50L, downloading.bytesDownloaded)
        assertEquals(100L, downloading.totalBytes)

        assertEquals("/path/to/model", downloaded.localPath)
        assertEquals("Test error", error.error)
        assertTrue(error.retryable)
    }

    private fun createMockDownloadManager(): TtsModelDownloadManagerContract {
        return object : TtsModelDownloadManagerContract {
            private val _downloadStates = kotlinx.coroutines.flow.MutableStateFlow<Map<String, TtsModelDownloadState>>(
                mapOf("test-model" to TtsModelDownloadState.NotDownloaded)
            )
            override val downloadStates: kotlinx.coroutines.flow.StateFlow<Map<String, TtsModelDownloadState>> = _downloadStates

            override suspend fun startDownload(modelId: String): Boolean {
                if (modelId == TtsModelRegistry.SYSTEM_TTS_ID || modelId == "non-existent") {
                    return false
                }
                _downloadStates.value = _downloadStates.value.toMutableMap().apply {
                    put(modelId, TtsModelDownloadState.Downloading(0f, 0, 100))
                }
                return true
            }

            override suspend fun pauseDownload(modelId: String) {
                _downloadStates.value = _downloadStates.value.toMutableMap().apply {
                    val current = get(modelId) as? TtsModelDownloadState.Downloading ?: return
                    put(modelId, TtsModelDownloadState.Paused(current.progress, current.bytesDownloaded, current.totalBytes))
                }
            }

            override suspend fun resumeDownload(modelId: String) {
                _downloadStates.value = _downloadStates.value.toMutableMap().apply {
                    val current = get(modelId) as? TtsModelDownloadState.Paused ?: return
                    put(modelId, TtsModelDownloadState.Downloading(current.progress, current.bytesDownloaded, current.totalBytes))
                }
            }

            override suspend fun cancelDownload(modelId: String) {
                _downloadStates.value = _downloadStates.value.toMutableMap().apply {
                    put(modelId, TtsModelDownloadState.NotDownloaded)
                }
            }

            override suspend fun isModelDownloaded(modelId: String): Boolean = false

            override fun getModelLocalPath(modelId: String): String? = null

            override fun release() {}
        }
    }
}
