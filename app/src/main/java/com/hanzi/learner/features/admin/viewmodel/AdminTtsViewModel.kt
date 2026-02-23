package com.hanzi.learner.features.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hanzi.learner.data.repository.TtsPreferenceRepositoryContract
import com.hanzi.learner.speech.contract.PreviewAudioPlayerContract
import com.hanzi.learner.speech.contract.TtsEngineInfo
import com.hanzi.learner.speech.contract.TtsModelDownloadManagerContract
import com.hanzi.learner.speech.contract.TtsModelRepositoryContract
import com.hanzi.learner.speech.contract.TtsSpeakerContract
import com.hanzi.learner.speech.model.TtsModelDownloadState
import com.hanzi.learner.speech.model.TtsModelUiItem
import com.hanzi.learner.speech.model.TtsSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class AdminTtsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val models: List<TtsModelUiItem> = emptyList(),
    val isPlayingPreview: Boolean = false,
    val currentlyPlayingModelId: String? = null,
    val settings: TtsSettings = TtsSettings.DEFAULT,
    val currentEngine: TtsEngineInfo? = null,
    val availableEngines: List<TtsEngineInfo> = emptyList(),
    val isChineseSupported: Boolean = false,
)

class AdminTtsViewModel(
    private val preferenceRepository: TtsPreferenceRepositoryContract,
    private val downloadManager: TtsModelDownloadManagerContract,
    private val previewPlayer: PreviewAudioPlayerContract,
    private val ttsSpeaker: TtsSpeakerContract,
    private val modelRepository: TtsModelRepositoryContract,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminTtsUiState())
    val uiState: StateFlow<AdminTtsUiState> = _uiState.asStateFlow()

    init {
        observeModels()
        observePreviewPlayer()
        observeSettings()
        observeTtsSpeaker()
    }

    private fun observeModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                combine(
                    downloadManager.downloadStates,
                    preferenceRepository.getSelectedModelId(),
                ) { downloadStates, selectedModelId ->
                    modelRepository.getAvailableModels().map { model ->
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

    private fun observePreviewPlayer() {
        viewModelScope.launch {
            previewPlayer.isPlaying.collect { isPlaying ->
                _uiState.value = _uiState.value.copy(
                    isPlayingPreview = isPlaying,
                    currentlyPlayingModelId = if (isPlaying) {
                        _uiState.value.currentlyPlayingModelId
                    } else {
                        null
                    },
                )
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            preferenceRepository.getSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
    }

    private fun observeTtsSpeaker() {
        viewModelScope.launch {
            combine(
                ttsSpeaker.currentEngine,
                ttsSpeaker.availableEngines,
                ttsSpeaker.isChineseSupported
            ) { engine, engines, chineseSupported ->
                Triple(engine, engines, chineseSupported)
            }.collect { (engine, engines, chineseSupported) ->
                _uiState.value = _uiState.value.copy(
                    currentEngine = engine,
                    availableEngines = engines,
                    isChineseSupported = chineseSupported,
                )
            }
        }
    }

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

    fun pauseDownload(modelId: String) {
        viewModelScope.launch {
            downloadManager.pauseDownload(modelId)
        }
    }

    fun resumeDownload(modelId: String) {
        viewModelScope.launch {
            downloadManager.resumeDownload(modelId)
        }
    }

    fun cancelDownload(modelId: String) {
        viewModelScope.launch {
            downloadManager.cancelDownload(modelId)
        }
    }

    fun playPreview(modelId: String) {
        viewModelScope.launch {
            try {
                previewPlayer.stop()

                val model = modelRepository.getModelById(modelId) ?: return@launch

                if (model.isSystemTts) {
                    _uiState.value = _uiState.value.copy(
                        currentlyPlayingModelId = modelId,
                    )
                    previewPlayer.playSystemTtsPreview(TtsModelRepositoryContract.PREVIEW_TEXT)
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
                            text = TtsModelRepositoryContract.PREVIEW_TEXT,
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

    fun stopPreview() {
        previewPlayer.stop()
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            preferenceRepository.setSpeechRate(rate)
            ttsSpeaker.setSpeechRate(rate)
        }
    }

    fun setPitch(pitch: Float) {
        viewModelScope.launch {
            preferenceRepository.setPitch(pitch)
            ttsSpeaker.setPitch(pitch)
        }
    }

    fun playSettingsPreview() {
        ttsSpeaker.speak("你好，这是语音设置预览。")
    }

    fun setEngine(enginePackageName: String) {
        ttsSpeaker.setEngine(enginePackageName)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        previewPlayer.stop()
    }

    class Factory(
        private val preferenceRepository: TtsPreferenceRepositoryContract,
        private val downloadManager: TtsModelDownloadManagerContract,
        private val previewPlayer: PreviewAudioPlayerContract,
        private val ttsSpeaker: TtsSpeakerContract,
        private val modelRepository: TtsModelRepositoryContract,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminTtsViewModel::class.java)) {
                return AdminTtsViewModel(
                    preferenceRepository = preferenceRepository,
                    downloadManager = downloadManager,
                    previewPlayer = previewPlayer,
                    ttsSpeaker = ttsSpeaker,
                    modelRepository = modelRepository,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
