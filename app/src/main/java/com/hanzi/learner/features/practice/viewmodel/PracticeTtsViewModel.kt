package com.hanzi.learner.features.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hanzi.learner.data.repository.TtsPreferenceRepositoryContract
import com.hanzi.learner.speech.contract.PreviewAudioPlayerContract
import com.hanzi.learner.speech.contract.TtsModelDownloadManagerContract
import com.hanzi.learner.speech.contract.TtsModelRepositoryContract
import com.hanzi.learner.speech.model.TtsModelDownloadState
import com.hanzi.learner.speech.model.TtsModelUiItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class PracticeTtsUiState(
    val showSelectionSheet: Boolean = false,
    val hasPrompted: Boolean = false,
    val models: List<TtsModelUiItem> = emptyList(),
    val isPlayingPreview: Boolean = false,
    val currentlyPlayingModelId: String? = null,
    val error: String? = null,
)

class PracticeTtsViewModel(
    private val preferenceRepository: TtsPreferenceRepositoryContract,
    private val downloadManager: TtsModelDownloadManagerContract,
    private val previewPlayer: PreviewAudioPlayerContract,
    private val modelRepository: TtsModelRepositoryContract,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PracticeTtsUiState())
    val uiState: StateFlow<PracticeTtsUiState> = _uiState.asStateFlow()

    init {
        observeModelsAndSelection()
        observePreview()
    }

    fun selectModel(modelId: String) {
        viewModelScope.launch {
            try {
                val model = modelRepository.getModelById(modelId) ?: return@launch
                if (!model.isSystemTts) {
                    val downloadState = downloadManager.downloadStates.value[modelId]
                    if (downloadState is TtsModelDownloadState.NotDownloaded ||
                        downloadState is TtsModelDownloadState.Paused ||
                        downloadState is TtsModelDownloadState.Error
                    ) {
                        downloadManager.startDownload(modelId)
                    }
                }
                preferenceRepository.setSelectedModelId(modelId)
                _uiState.value = _uiState.value.copy(showSelectionSheet = false, error = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to select model")
            }
        }
    }

    fun dismissSelection() {
        _uiState.value = _uiState.value.copy(showSelectionSheet = false)
    }

    fun playPreview(modelId: String) {
        viewModelScope.launch {
            try {
                previewPlayer.stop()
                val model = modelRepository.getModelById(modelId) ?: return@launch
                _uiState.value = _uiState.value.copy(currentlyPlayingModelId = modelId, error = null)
                if (model.isSystemTts) {
                    previewPlayer.playSystemTtsPreview("你好，这是系统语音预览")
                } else {
                    previewPlayer.playFromUrl(model.previewAudioUrl)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to play preview",
                    currentlyPlayingModelId = null,
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        previewPlayer.release()
    }

    private fun observeModelsAndSelection() {
        viewModelScope.launch {
            combine(
                preferenceRepository.getSelectedModelId(),
                downloadManager.downloadStates,
            ) { selectedId, downloadStates ->
                val models = modelRepository.getAvailableModels().map { model ->
                    TtsModelUiItem(
                        info = model,
                        downloadState = downloadStates[model.id] ?: TtsModelDownloadState.NotDownloaded,
                        isSelected = model.id == selectedId,
                    )
                }
                selectedId to models
            }.collect { (selectedId, models) ->
                val prompted = _uiState.value.hasPrompted
                val shouldPrompt = selectedId == null && models.isNotEmpty() && !prompted
                _uiState.value = _uiState.value.copy(
                    models = models,
                    showSelectionSheet = shouldPrompt || _uiState.value.showSelectionSheet && selectedId == null,
                    hasPrompted = prompted || shouldPrompt,
                )
                if (selectedId != null) {
                    _uiState.value = _uiState.value.copy(showSelectionSheet = false, hasPrompted = false)
                }
            }
        }
    }

    private fun observePreview() {
        viewModelScope.launch {
            previewPlayer.isPlaying.collect { playing ->
                _uiState.value = _uiState.value.copy(
                    isPlayingPreview = playing,
                    currentlyPlayingModelId = if (playing) _uiState.value.currentlyPlayingModelId else null,
                )
            }
        }
    }

    class Factory(
        private val preferenceRepository: TtsPreferenceRepositoryContract,
        private val downloadManager: TtsModelDownloadManagerContract,
        private val previewPlayer: PreviewAudioPlayerContract,
        private val modelRepository: TtsModelRepositoryContract,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PracticeTtsViewModel::class.java)) {
                return PracticeTtsViewModel(
                    preferenceRepository = preferenceRepository,
                    downloadManager = downloadManager,
                    previewPlayer = previewPlayer,
                    modelRepository = modelRepository,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
