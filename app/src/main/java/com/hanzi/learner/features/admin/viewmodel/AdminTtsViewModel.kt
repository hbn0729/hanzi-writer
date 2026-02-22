package com.hanzi.learner.features.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hanzi.learner.data.repository.TtsPreferenceRepositoryContract
import com.hanzi.learner.speech.contract.PreviewAudioPlayerContract
import com.hanzi.learner.speech.contract.TtsModelDownloadManagerContract
import com.hanzi.learner.speech.model.TtsModelDownloadState
import com.hanzi.learner.speech.model.TtsModelRegistry
import com.hanzi.learner.speech.model.TtsModelUiItem
import com.hanzi.learner.speech.internal.PREVIEW_TEXT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * UI state for the TTS management screen.
 */
data class AdminTtsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val models: List<TtsModelUiItem> = emptyList(),
    val isPlayingPreview: Boolean = false,
    val currentlyPlayingModelId: String? = null,
)

/**
 * ViewModel for TTS model management in Admin screen.
 * Manages model selection, download, and preview playback.
 */
class AdminTtsViewModel(
    private val preferenceRepository: TtsPreferenceRepositoryContract,
    private val downloadManager: TtsModelDownloadManagerContract,
    private val previewPlayer: PreviewAudioPlayerContract,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminTtsUiState())
    val uiState: StateFlow<AdminTtsUiState> = _uiState.asStateFlow()

    init {
        observeModels()
        observePreviewPlayer()
    }

    /**
     * Combines model registry, download states, and user preference into UI state.
     */
    private fun observeModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                combine(
                    downloadManager.downloadStates,
                    preferenceRepository.getSelectedModelId(),
                ) { downloadStates, selectedModelId ->
                    TtsModelRegistry.getAvailableModels().map { model ->
                        TtsModelUiItem(
                            info = model,
                            downloadState = downloadStates[model.id] ?: TtsModelDownloadState.NotDownloaded,
                            isSelected = model.id == selectedModelId,
                        )
                    }
                }.collect { models ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        models = models,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load TTS models",
                )
            }
        }
    }

    /**
     * Observes preview player state to update UI.
     */
    private fun observePreviewPlayer() {
        viewModelScope.launch {
            previewPlayer.isPlaying.collect { isPlaying ->
                _uiState.value = _uiState.value.copy(
                    isPlayingPreview = isPlaying,
                    // Clear currently playing model when stopped
                    currentlyPlayingModelId = if (isPlaying) {
                        _uiState.value.currentlyPlayingModelId
                    } else {
                        null
                    },
                )
            }
        }
    }

    /**
     * Selects a model as the active TTS engine.
     */
    fun selectModel(modelId: String) {
        viewModelScope.launch {
            try {
                preferenceRepository.setSelectedModelId(modelId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to select model",
                )
            }
        }
    }

    /**
     * Starts downloading a model.
     */
    fun startDownload(modelId: String) {
        viewModelScope.launch {
            try {
                val started = downloadManager.startDownload(modelId)
                if (!started) {
                    _uiState.value = _uiState.value.copy(
                        error = "Download already in progress or model not found",
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to start download",
                )
            }
        }
    }

    /**
     * Pauses an ongoing download.
     */
    fun pauseDownload(modelId: String) {
        viewModelScope.launch {
            downloadManager.pauseDownload(modelId)
        }
    }

    /**
     * Resumes a paused download.
     */
    fun resumeDownload(modelId: String) {
        viewModelScope.launch {
            downloadManager.resumeDownload(modelId)
        }
    }

    /**
     * Cancels and removes a download.
     */
    fun cancelDownload(modelId: String) {
        viewModelScope.launch {
            downloadManager.cancelDownload(modelId)
        }
    }

    /**
     * Plays the preview audio for a model.
     * For system TTS: uses system TTS directly.
     * For downloadable models: requires model to be downloaded first.
     */
    fun playPreview(modelId: String) {
        viewModelScope.launch {
            try {
                previewPlayer.stop()

                val model = TtsModelRegistry.getModelById(modelId) ?: return@launch

                if (model.isSystemTts) {
                    _uiState.value = _uiState.value.copy(
                        currentlyPlayingModelId = modelId,
                    )
                    previewPlayer.playSystemTtsPreview(PREVIEW_TEXT)
                    return@launch
                }

                val downloadState = downloadManager.downloadStates.value[modelId]
                    ?: TtsModelDownloadState.NotDownloaded

                when (downloadState) {
                    is TtsModelDownloadState.NotDownloaded -> {
                        _uiState.value = _uiState.value.copy(
                            error = "请先下载模型后再试听",
                        )
                    }
                    is TtsModelDownloadState.Downloading -> {
                        _uiState.value = _uiState.value.copy(
                            error = "模型下载中，请等待下载完成后再试听",
                        )
                    }
                    is TtsModelDownloadState.Paused -> {
                        _uiState.value = _uiState.value.copy(
                            error = "模型下载已暂停，请继续下载后再试听",
                        )
                    }
                    is TtsModelDownloadState.Downloaded -> {
                        _uiState.value = _uiState.value.copy(
                            currentlyPlayingModelId = modelId,
                        )
                        previewPlayer.playFromLocalModel(
                            modelDirPath = downloadState.localPath,
                            modelFiles = model.modelFiles,
                            text = PREVIEW_TEXT,
                        )
                    }
                    is TtsModelDownloadState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = "模型下载失败，请重试下载后再试听",
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "试听失败",
                    currentlyPlayingModelId = null,
                )
            }
        }
    }

    /**
     * Stops the current preview playback.
     */
    fun stopPreview() {
        previewPlayer.stop()
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        previewPlayer.release()
    }

    /**
     * Factory for creating AdminTtsViewModel instances.
     */
    class Factory(
        private val preferenceRepository: TtsPreferenceRepositoryContract,
        private val downloadManager: TtsModelDownloadManagerContract,
        private val previewPlayer: PreviewAudioPlayerContract,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminTtsViewModel::class.java)) {
                return AdminTtsViewModel(
                    preferenceRepository = preferenceRepository,
                    downloadManager = downloadManager,
                    previewPlayer = previewPlayer,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
