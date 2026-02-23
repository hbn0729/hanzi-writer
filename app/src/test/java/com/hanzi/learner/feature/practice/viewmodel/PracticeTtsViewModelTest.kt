package com.hanzi.learner.features.practice.viewmodel

import com.hanzi.learner.data.repository.TtsPreferenceRepositoryContract
import com.hanzi.learner.speech.contract.PreviewAudioPlayerContract
import com.hanzi.learner.speech.contract.TtsModelDownloadManagerContract
import com.hanzi.learner.speech.model.TtsModelDownloadState
import com.hanzi.learner.speech.model.TtsModelRegistry
import com.hanzi.learner.speech.model.TtsSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeTtsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `no preference prompts selection sheet once`() = runTest {
        val preference = FakePreferenceRepository(initial = null)
        val downloadManager = FakeDownloadManager()
        val previewPlayer = FakePreviewPlayer()

        val viewModel = PracticeTtsViewModel(
            preferenceRepository = preference,
            downloadManager = downloadManager,
            previewPlayer = previewPlayer,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.models.isNotEmpty())
        assertTrue(state.showSelectionSheet)

        viewModel.dismissSelection()
        assertFalse(viewModel.uiState.value.showSelectionSheet)
    }

    @Test
    fun `selecting system tts hides sheet and sets preference`() = runTest {
        val preference = FakePreferenceRepository(initial = null)
        val downloadManager = FakeDownloadManager()
        val previewPlayer = FakePreviewPlayer()

        val viewModel = PracticeTtsViewModel(
            preferenceRepository = preference,
            downloadManager = downloadManager,
            previewPlayer = previewPlayer,
        )

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showSelectionSheet)

        viewModel.selectModel(TtsModelRegistry.SYSTEM_TTS_ID)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showSelectionSheet)
        assertTrue(preference.selected.value == TtsModelRegistry.SYSTEM_TTS_ID)
    }

    private class FakePreferenceRepository(initial: String?) : TtsPreferenceRepositoryContract {
        val selected = MutableStateFlow<String?>(initial)
        private val settings = MutableStateFlow(TtsSettings.DEFAULT)

        override fun getSelectedModelId(): Flow<String?> = selected
        override suspend fun setSelectedModelId(modelId: String?) {
            selected.value = modelId
        }
        override suspend fun clearSelection() {
            selected.value = null
        }
        override fun getSettings(): Flow<TtsSettings> = settings
        override suspend fun setSpeechRate(rate: Float) {
            settings.value = settings.value.copy(speechRate = rate)
        }
        override suspend fun setPitch(pitch: Float) {
            settings.value = settings.value.copy(pitch = pitch)
        }
        override suspend fun updateSettings(newSettings: TtsSettings) {
            settings.value = newSettings
        }
    }

    private class FakeDownloadManager : TtsModelDownloadManagerContract {
        private val _states = MutableStateFlow<Map<String, TtsModelDownloadState>>(emptyMap())
        override val downloadStates: StateFlow<Map<String, TtsModelDownloadState>> = _states

        override suspend fun startDownload(modelId: String): Boolean {
            _states.value = _states.value.toMutableMap().apply {
                put(modelId, TtsModelDownloadState.Downloading(0f, 0L, 1L))
            }
            return true
        }

        override suspend fun pauseDownload(modelId: String) {}
        override suspend fun resumeDownload(modelId: String) {}
        override suspend fun cancelDownload(modelId: String) {}
        override suspend fun isModelDownloaded(modelId: String): Boolean =
            downloadStates.value[modelId] is TtsModelDownloadState.Downloaded

        override fun getModelLocalPath(modelId: String): String? =
            (downloadStates.value[modelId] as? TtsModelDownloadState.Downloaded)?.localPath

        override fun release() {}
    }

    private class FakePreviewPlayer : PreviewAudioPlayerContract {
        private val _isPlaying = MutableStateFlow(false)
        override val isPlaying: StateFlow<Boolean> = _isPlaying

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
