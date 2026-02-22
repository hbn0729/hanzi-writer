package com.hanzi.learner.speech.internal

import android.content.Context
import com.hanzi.learner.data.repository.TtsPreferenceRepositoryContract
import com.hanzi.learner.speech.SpeechModule
import com.hanzi.learner.speech.contract.TtsModelDownloadManagerContract
import com.hanzi.learner.speech.contract.TtsSpeakerContract
import com.hanzi.learner.speech.model.TtsModelDownloadState
import com.hanzi.learner.speech.model.TtsModelRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal class PreferenceTtsSpeaker(
    context: Context,
    preferenceRepository: TtsPreferenceRepositoryContract,
    downloadManager: TtsModelDownloadManagerContract,
) : TtsSpeakerContract {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val _isReady = MutableStateFlow(true)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private var activeSpeaker: TtsSpeakerContract = NoOpTtsSpeaker()
    private var readinessJob: Job? = null

    private data class Selection(
        val modelId: String?,
        val downloadedPath: String?,
    )

    init {
        scope.launch {
            combine(
                preferenceRepository.getSelectedModelId(),
                downloadManager.downloadStates,
            ) { selectedId, states ->
                val downloaded = if (selectedId != null) {
                    (states[selectedId] as? TtsModelDownloadState.Downloaded)?.localPath
                } else {
                    null
                }
                Selection(selectedId, downloaded)
            }
                .distinctUntilChanged()
                .collect { selection ->
                    switchTo(selection)
                }
        }

        attachReadiness(activeSpeaker)
    }

    override fun speak(text: String) {
        activeSpeaker.speak(text)
    }

    override fun speakCharacterAndPhrase(character: String, phrase: String) {
        activeSpeaker.speakCharacterAndPhrase(character, phrase)
    }

    override fun shutdown() {
        readinessJob?.cancel()
        readinessJob = null
        activeSpeaker.shutdown()
        scope.cancel()
    }

    private fun switchTo(selection: Selection) {
        val next = when {
            selection.modelId == null -> NoOpTtsSpeaker()
            selection.modelId == TtsModelRegistry.SYSTEM_TTS_ID -> SystemTtsSpeaker(appContext)
            selection.downloadedPath != null -> {
                val modelInfo = TtsModelRegistry.getModelById(selection.modelId) ?: return
                SpeechModule.createFilesystemTtsSpeaker(
                    context = appContext,
                    modelDirPath = selection.downloadedPath,
                    modelFiles = modelInfo.modelFiles,
                )
            }
            else -> NoOpTtsSpeaker()
        }

        activeSpeaker.shutdown()
        activeSpeaker = next
        attachReadiness(next)
    }

    private fun attachReadiness(speaker: TtsSpeakerContract) {
        readinessJob?.cancel()
        readinessJob = scope.launch {
            speaker.isReady.collect { ready ->
                _isReady.value = ready
            }
        }
    }
}
