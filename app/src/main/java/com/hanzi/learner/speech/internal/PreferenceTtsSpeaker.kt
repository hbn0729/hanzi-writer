package com.hanzi.learner.speech.internal

import android.content.Context
import com.hanzi.learner.data.repository.TtsPreferenceRepositoryContract
import com.hanzi.learner.speech.SpeechModule
import com.hanzi.learner.speech.contract.TtsEngineInfo
import com.hanzi.learner.speech.contract.TtsModelDownloadManagerContract
import com.hanzi.learner.speech.contract.TtsSpeakerContract
import com.hanzi.learner.speech.contract.TtsVoiceInfo
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

    private val _speechRate = MutableStateFlow(1.0f)
    override val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _pitch = MutableStateFlow(1.0f)
    override val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _currentEngine = MutableStateFlow<TtsEngineInfo?>(null)
    override val currentEngine: StateFlow<TtsEngineInfo?> = _currentEngine.asStateFlow()

    private val _availableEngines = MutableStateFlow<List<TtsEngineInfo>>(emptyList())
    override val availableEngines: StateFlow<List<TtsEngineInfo>> = _availableEngines.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceInfo>>(emptyList())
    override val availableVoices: StateFlow<List<TtsVoiceInfo>> = _availableVoices.asStateFlow()

    private val _isChineseSupported = MutableStateFlow(false)
    override val isChineseSupported: StateFlow<Boolean> = _isChineseSupported.asStateFlow()

    private var activeSpeaker: TtsSpeakerContract = NoOpTtsSpeaker()
    private var stateObservationJobs: MutableList<Job> = mutableListOf()

    init {
        observeSpeakerSelection(preferenceRepository, downloadManager)
    }

    private fun observeSpeakerSelection(
        preferenceRepository: TtsPreferenceRepositoryContract,
        downloadManager: TtsModelDownloadManagerContract,
    ) {
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
                SpeakerSelection(selectedId, downloaded)
            }
                .distinctUntilChanged()
                .collect { selection ->
                    switchToSpeaker(selection)
                }
        }
    }

    private fun switchToSpeaker(selection: SpeakerSelection) {
        val nextSpeaker = createSpeakerForSelection(selection)

        activeSpeaker.shutdown()
        activeSpeaker = nextSpeaker
        observeSpeakerState(nextSpeaker)
    }

    private fun createSpeakerForSelection(selection: SpeakerSelection): TtsSpeakerContract {
        return when {
            selection.modelId == null -> NoOpTtsSpeaker()
            selection.modelId == TtsModelRegistry.SYSTEM_TTS_ID -> SystemTtsSpeaker(appContext)
            selection.downloadedPath != null -> {
                val modelInfo = TtsModelRegistry.getModelById(selection.modelId) ?: return NoOpTtsSpeaker()
                SpeechModule.createFilesystemTtsSpeaker(
                    context = appContext,
                    modelDirPath = selection.downloadedPath,
                    modelFiles = modelInfo.modelFiles,
                )
            }
            else -> NoOpTtsSpeaker()
        }
    }

    private fun observeSpeakerState(speaker: TtsSpeakerContract) {
        stateObservationJobs.forEach { it.cancel() }
        stateObservationJobs.clear()

        val coreStateJob = scope.launch {
            combine(
                speaker.isReady,
                speaker.speechRate,
                speaker.pitch,
                speaker.currentEngine,
                speaker.availableEngines,
            ) { ready, rate, pitch, engine, engines ->
                SpeakerCoreState(ready, rate, pitch, engine, engines)
            }.collect { state ->
                _isReady.value = state.ready
                _speechRate.value = state.rate
                _pitch.value = state.pitch
                _currentEngine.value = state.engine
                _availableEngines.value = state.engines
            }
        }
        stateObservationJobs.add(coreStateJob)

        val voiceStateJob = scope.launch {
            combine(
                speaker.availableVoices,
                speaker.isChineseSupported,
            ) { voices, chinese ->
                voices to chinese
            }.collect { (voices, chinese) ->
                _availableVoices.value = voices
                _isChineseSupported.value = chinese
            }
        }
        stateObservationJobs.add(voiceStateJob)
    }

    override fun speak(text: String) {
        activeSpeaker.speak(text)
    }

    override fun speakCharacterAndPhrase(character: String, phrase: String) {
        activeSpeaker.speakCharacterAndPhrase(character, phrase)
    }

    override fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        activeSpeaker.setSpeechRate(rate)
    }

    override fun setPitch(pitch: Float) {
        _pitch.value = pitch
        activeSpeaker.setPitch(pitch)
    }

    override fun stop() {
        activeSpeaker.stop()
    }

    override fun shutdown() {
        stateObservationJobs.forEach { it.cancel() }
        stateObservationJobs.clear()
        activeSpeaker.shutdown()
        scope.cancel()
    }

    private data class SpeakerSelection(
        val modelId: String?,
        val downloadedPath: String?,
    )

    private data class SpeakerCoreState(
        val ready: Boolean,
        val rate: Float,
        val pitch: Float,
        val engine: TtsEngineInfo?,
        val engines: List<TtsEngineInfo>,
    )
}
