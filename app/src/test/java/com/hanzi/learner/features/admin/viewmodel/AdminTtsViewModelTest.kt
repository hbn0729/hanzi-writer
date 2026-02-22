package com.hanzi.learner.features.admin.viewmodel

import com.hanzi.learner.speech.model.TtsModelDownloadState
import com.hanzi.learner.speech.model.TtsModelInfo
import com.hanzi.learner.speech.model.TtsModelRegistry
import com.hanzi.learner.speech.model.TtsModelUiItem
import org.junit.Assert.*
import org.junit.Test

class AdminTtsViewModelTest {

    @Test
    fun `TtsModelUiItem should hold correct data`() {
        val modelInfo = TtsModelInfo(
            id = "test-model",
            name = "Test Model",
            description = "Test description",
            downloadUrl = "https://example.com/model.zip",
            previewAudioUrl = "https://example.com/preview.mp3",
            fileSizeBytes = 1024 * 1024 * 50,
            modelFiles = listOf("model.onnx"),
            isSystemTts = false,
        )
        val downloadState = TtsModelDownloadState.Downloading(0.5f, 25 * 1024 * 1024, 50 * 1024 * 1024)

        val item = TtsModelUiItem(
            info = modelInfo,
            downloadState = downloadState,
            isSelected = true,
        )

        assertEquals(modelInfo, item.info)
        assertEquals(downloadState, item.downloadState)
        assertTrue(item.isSelected)
    }

    @Test
    fun `AdminTtsUiState should have correct default values`() {
        val state = AdminTtsUiState()

        assertFalse(state.isLoading)
        assertNull(state.error)
        assertTrue(state.models.isEmpty())
        assertFalse(state.isPlayingPreview)
        assertNull(state.currentlyPlayingModelId)
    }

    @Test
    fun `AdminTtsUiState should support custom values`() {
        val models = listOf(
            TtsModelUiItem(
                info = TtsModelInfo(
                    id = "test-model",
                    name = "Test Model",
                    description = "Test",
                    downloadUrl = null,
                    previewAudioUrl = "",
                    fileSizeBytes = 0,
                    modelFiles = emptyList(),
                    isSystemTts = true,
                ),
                downloadState = TtsModelDownloadState.NotDownloaded,
                isSelected = true,
            )
        )

        val state = AdminTtsUiState(
            isLoading = true,
            error = "Test error",
            models = models,
            isPlayingPreview = true,
            currentlyPlayingModelId = "test-model",
        )

        assertTrue(state.isLoading)
        assertEquals("Test error", state.error)
        assertEquals(1, state.models.size)
        assertTrue(state.isPlayingPreview)
        assertEquals("test-model", state.currentlyPlayingModelId)
    }

    @Test
    fun `TtsModelRegistry should contain system TTS`() {
        val models = TtsModelRegistry.getAvailableModels()

        val systemTts = models.find { it.id == TtsModelRegistry.SYSTEM_TTS_ID }
        assertNotNull(systemTts)
        assertTrue(systemTts?.isSystemTts == true)
    }

    @Test
    fun `TtsModelDownloadState should have all required states`() {
        // Verify all states can be instantiated
        val notDownloaded = TtsModelDownloadState.NotDownloaded
        val downloading = TtsModelDownloadState.Downloading(0.5f, 50, 100)
        val paused = TtsModelDownloadState.Paused(0.3f, 30, 100)
        val downloaded = TtsModelDownloadState.Downloaded("/path/to/model")
        val error = TtsModelDownloadState.Error("Test error", retryable = true)

        assertNotNull(notDownloaded)
        assertNotNull(downloading)
        assertNotNull(paused)
        assertNotNull(downloaded)
        assertNotNull(error)

        // Verify properties
        assertEquals(0.5f, downloading.progress, 0.01f)
        assertEquals(50L, downloading.bytesDownloaded)
        assertEquals(100L, downloading.totalBytes)

        assertEquals(0.3f, paused.progress, 0.01f)
        assertEquals(30L, paused.bytesDownloaded)

        assertEquals("/path/to/model", downloaded.localPath)

        assertEquals("Test error", error.error)
        assertTrue(error.retryable)
    }

    @Test
    fun `TtsModelUiItem equality works correctly`() {
        val modelInfo = TtsModelInfo(
            id = "test",
            name = "Test",
            description = "Desc",
            downloadUrl = null,
            previewAudioUrl = "",
            fileSizeBytes = 0,
            modelFiles = emptyList(),
            isSystemTts = true,
        )

        val item1 = TtsModelUiItem(modelInfo, TtsModelDownloadState.NotDownloaded, true)
        val item2 = TtsModelUiItem(modelInfo, TtsModelDownloadState.NotDownloaded, true)
        val item3 = TtsModelUiItem(modelInfo, TtsModelDownloadState.NotDownloaded, false)

        assertEquals(item1, item2)
        assertNotEquals(item1, item3)
    }
}
