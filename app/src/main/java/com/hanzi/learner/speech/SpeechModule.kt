package com.hanzi.learner.speech

import android.content.Context
import com.hanzi.learner.data.repository.TtsPreferenceRepositoryContract
import com.hanzi.learner.speech.contract.AudioPlayerContract
import com.hanzi.learner.speech.contract.TtsEngineContract
import com.hanzi.learner.speech.contract.TtsModelDownloadManagerContract
import com.hanzi.learner.speech.contract.TtsModelRepositoryContract
import com.hanzi.learner.speech.contract.TtsSpeakerContract
import com.hanzi.learner.speech.internal.FallbackTtsSpeaker
import com.hanzi.learner.speech.internal.NoOpTtsSpeaker
import com.hanzi.learner.speech.internal.PcmFloatAudioPlayer
import com.hanzi.learner.speech.internal.SherpaOnnxTtsEngine
import com.hanzi.learner.speech.internal.SherpaOnnxTtsSpeaker
import com.hanzi.learner.speech.internal.SystemTtsSpeaker
import com.hanzi.learner.speech.model.TtsModelDownloadState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

object SpeechModule {

    data class TtsConfig(
        val modelPath: String = "",
        val tokensPath: String = "",
        val lexiconPath: String = "",
        val dictDir: String = "",
        val speakerId: Int = 0,
        val speed: Float = 1.0f,
        val useFilesystem: Boolean = false,
    )

    suspend fun createTtsSpeakerFromPreference(
        context: Context,
        preferenceRepository: TtsPreferenceRepositoryContract,
        downloadManager: TtsModelDownloadManagerContract? = null,
        modelRepository: TtsModelRepositoryContract = com.hanzi.learner.speech.model.TtsModelRegistry(),
    ): TtsSpeakerContract {
        val selectedModelId = preferenceRepository.getSelectedModelId().first()
        return createTtsSpeakerForModel(context, selectedModelId, downloadManager, modelRepository)
    }

    suspend fun createTtsSpeakerForModel(
        context: Context,
        modelId: String?,
        downloadManager: TtsModelDownloadManagerContract? = null,
        modelRepository: TtsModelRepositoryContract = com.hanzi.learner.speech.model.TtsModelRegistry(),
    ): TtsSpeakerContract {
        if (modelId == null) {
            return NoOpTtsSpeaker()
        }
        if (modelId == TtsModelRepositoryContract.SYSTEM_TTS_ID) {
            return SystemTtsSpeaker(context)
        }

        val modelInfo = modelRepository.getModelById(modelId)
            ?: return NoOpTtsSpeaker()

        if (modelInfo.isSystemTts) {
            return SystemTtsSpeaker(context)
        }

        val isDownloaded = downloadManager?.isModelDownloaded(modelId) == true

        if (!isDownloaded) {
            return NoOpTtsSpeaker()
        }

        val modelPath = downloadManager?.getModelLocalPath(modelId)
            ?: return NoOpTtsSpeaker()

        return createFilesystemTtsSpeaker(context, modelPath, modelInfo.modelFiles)
    }

    fun createTtsSpeaker(context: Context, config: TtsConfig = TtsConfig()): TtsSpeakerContract {
        val modelConfig = SherpaOnnxTtsEngine.TtsModelConfig(
            modelPath = config.modelPath,
            tokensPath = config.tokensPath,
            lexiconPath = config.lexiconPath,
            dictDir = config.dictDir,
            useFilesystem = config.useFilesystem,
        )

        val engine: TtsEngineContract = SherpaOnnxTtsEngine(
            assetManager = context.assets,
            modelConfig = modelConfig,
            defaultSpeakerId = config.speakerId,
            defaultSpeed = config.speed,
        )

        val player: AudioPlayerContract = PcmFloatAudioPlayer()

        return SherpaOnnxTtsSpeaker(
            engine = engine,
            player = player,
        )
    }

    fun createFilesystemTtsSpeaker(
        context: Context,
        modelDirPath: String,
        modelFiles: List<String>,
    ): TtsSpeakerContract {
        val onnxFile = modelFiles.firstOrNull { it.endsWith(".onnx") } ?: "model.onnx"
        val tokensFile = modelFiles.firstOrNull { it.endsWith("tokens.txt") } ?: "tokens.txt"
        val lexiconFile = modelFiles.firstOrNull { it.endsWith("lexicon.txt") } ?: "lexicon.txt"
        val dictDir = File(modelDirPath, "dict").takeIf { it.exists() && it.isDirectory }?.absolutePath.orEmpty()

        val config = TtsConfig(
            modelPath = "$modelDirPath/$onnxFile",
            tokensPath = "$modelDirPath/$tokensFile",
            lexiconPath = "$modelDirPath/$lexiconFile",
            dictDir = dictDir,
            useFilesystem = true,
        )

        return createTtsSpeaker(context, config)
    }

    fun createTtsSpeakerWithFallback(context: Context, config: TtsConfig = TtsConfig()): TtsSpeakerContract {
        return FallbackTtsSpeaker(context, config)
    }

    suspend fun isModelReady(
        modelId: String,
        downloadManager: TtsModelDownloadManagerContract,
    ): Boolean {
        if (modelId == TtsModelRepositoryContract.SYSTEM_TTS_ID) {
            return true
        }
        return downloadManager.isModelDownloaded(modelId)
    }

    fun getModelAvailabilityFlow(
        downloadManager: TtsModelDownloadManagerContract,
        modelRepository: TtsModelRepositoryContract = com.hanzi.learner.speech.model.TtsModelRegistry(),
    ): Flow<Map<String, Boolean>> {
        return downloadManager.downloadStates.map { states ->
            modelRepository.getAvailableModels().associate { model ->
                val isAvailable = if (model.isSystemTts) {
                    true
                } else {
                    states[model.id] is TtsModelDownloadState.Downloaded
                }
                model.id to isAvailable
            }
        }
    }
}
