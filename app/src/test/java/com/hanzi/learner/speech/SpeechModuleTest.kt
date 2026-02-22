package com.hanzi.learner.speech

import com.hanzi.learner.data.repository.TtsPreferenceRepositoryContract
import com.hanzi.learner.speech.contract.TtsModelDownloadManagerContract
import com.hanzi.learner.speech.model.TtsModelDownloadState
import com.hanzi.learner.speech.model.TtsModelRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SpeechModuleTest {

    @Test
    fun `TtsConfig should have correct default values`() {
        val config = SpeechModule.TtsConfig()

        assertEquals("", config.modelPath)
        assertEquals("", config.tokensPath)
        assertEquals("", config.lexiconPath)
        assertEquals("", config.dictDir)
        assertEquals(0, config.speakerId)
        assertEquals(1.0f, config.speed, 0.01f)
        assertFalse(config.useFilesystem)
    }

    @Test
    fun `TtsConfig should support filesystem loading`() {
        val config = SpeechModule.TtsConfig(
            modelPath = "/data/model.onnx",
            useFilesystem = true,
        )

        assertTrue(config.useFilesystem)
        assertEquals("/data/model.onnx", config.modelPath)
    }

    @Test
    fun `isModelReady should return true for system TTS`() = runTest {
        val mockDownloadManager = createMockDownloadManager()

        val result = SpeechModule.isModelReady(TtsModelRegistry.SYSTEM_TTS_ID, mockDownloadManager)

        assertTrue(result)
    }

    @Test
    fun `isModelReady should check download state for non-system models`() = runTest {
        val mockDownloadManager = object : TtsModelDownloadManagerContract {
            override val downloadStates: StateFlow<Map<String, TtsModelDownloadState>> = MutableStateFlow(
                mapOf("test-model" to TtsModelDownloadState.Downloaded("/path"))
            )
            override suspend fun startDownload(modelId: String): Boolean = true
            override suspend fun pauseDownload(modelId: String) {}
            override suspend fun resumeDownload(modelId: String) {}
            override suspend fun cancelDownload(modelId: String) {}
            override suspend fun isModelDownloaded(modelId: String): Boolean = modelId == "test-model"
            override fun getModelLocalPath(modelId: String): String? = "/path"
            override fun release() {}
        }

        assertTrue(SpeechModule.isModelReady("test-model", mockDownloadManager))
        assertFalse(SpeechModule.isModelReady("other-model", mockDownloadManager))
    }

    @Test
    fun `isModelReady should return false for non-downloaded model`() = runTest {
        val mockDownloadManager = object : TtsModelDownloadManagerContract {
            override val downloadStates: StateFlow<Map<String, TtsModelDownloadState>> = MutableStateFlow(emptyMap())
            override suspend fun startDownload(modelId: String): Boolean = true
            override suspend fun pauseDownload(modelId: String) {}
            override suspend fun resumeDownload(modelId: String) {}
            override suspend fun cancelDownload(modelId: String) {}
            override suspend fun isModelDownloaded(modelId: String): Boolean = false
            override fun getModelLocalPath(modelId: String): String? = null
            override fun release() {}
        }

        assertFalse(SpeechModule.isModelReady("vits-zh-hf-fanchen-wnj", mockDownloadManager))
    }

    @Test
    fun `TtsModelRegistry should contain expected models`() {
        val models = TtsModelRegistry.getAvailableModels()

        // Should have at least system TTS and some downloadable models
        assertTrue(models.isNotEmpty())

        // Should have system TTS
        val systemTts = models.find { it.isSystemTts }
        assertNotNull(systemTts)
        assertEquals(TtsModelRegistry.SYSTEM_TTS_ID, systemTts?.id)

        // All models should have required fields
        models.forEach { model ->
            assertNotNull(model.id)
            assertNotNull(model.name)
            assertNotNull(model.description)
            assertNotNull(model.previewAudioUrl)
            assertTrue(model.fileSizeBytes >= 0)

            if (!model.isSystemTts) {
                assertNotNull(model.downloadUrl)
                assertTrue(model.modelFiles.isNotEmpty())
            }
        }
    }

    @Test
    fun `getModelById should return correct model`() {
        val systemTts = TtsModelRegistry.getModelById(TtsModelRegistry.SYSTEM_TTS_ID)
        assertNotNull(systemTts)
        assertTrue(systemTts?.isSystemTts == true)

        val nonExistent = TtsModelRegistry.getModelById("non-existent")
        assertNull(nonExistent)
    }

    private fun createMockDownloadManager(): TtsModelDownloadManagerContract {
        return object : TtsModelDownloadManagerContract {
            override val downloadStates: StateFlow<Map<String, TtsModelDownloadState>> = MutableStateFlow(emptyMap())
            override suspend fun startDownload(modelId: String): Boolean = true
            override suspend fun pauseDownload(modelId: String) {}
            override suspend fun resumeDownload(modelId: String) {}
            override suspend fun cancelDownload(modelId: String) {}
            override suspend fun isModelDownloaded(modelId: String): Boolean = false
            override fun getModelLocalPath(modelId: String): String? = null
            override fun release() {}
        }
    }
}
