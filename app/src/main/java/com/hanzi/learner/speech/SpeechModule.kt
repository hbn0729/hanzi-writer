package com.hanzi.learner.speech

import android.content.Context
import com.hanzi.learner.data.repository.TtsPreferenceRepositoryContract
import com.hanzi.learner.speech.contract.AudioPlayerContract
import com.hanzi.learner.speech.contract.TtsEngineContract
import com.hanzi.learner.speech.contract.TtsModelDownloadManagerContract
import com.hanzi.learner.speech.contract.TtsSpeakerContract
import com.hanzi.learner.speech.internal.FallbackTtsSpeaker
import com.hanzi.learner.speech.internal.NoOpTtsSpeaker
import com.hanzi.learner.speech.internal.PcmFloatAudioPlayer
import com.hanzi.learner.speech.internal.SherpaOnnxTtsEngine
import com.hanzi.learner.speech.internal.SherpaOnnxTtsSpeaker
import com.hanzi.learner.speech.internal.SystemTtsSpeaker
import com.hanzi.learner.speech.model.TtsModelDownloadState
import com.hanzi.learner.speech.model.TtsModelRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Speech module for creating TTS speakers.
 * Supports both built-in SherpaOnnx models and system TTS.
 */
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

    /**
     * Creates a TTS speaker based on user preference.
     * Falls back to system TTS if the selected model is not available.
     *
     * @param context Application context
     * @param preferenceRepository Repository for accessing user TTS preferences
     * @param downloadManager Download manager for checking model availability
     * @return TtsSpeakerContract implementation
     */
    suspend fun createTtsSpeakerFromPreference(
        context: Context,
        preferenceRepository: TtsPreferenceRepositoryContract,
        downloadManager: TtsModelDownloadManagerContract? = null,
    ): TtsSpeakerContract {
        val selectedModelId = preferenceRepository.getSelectedModelId().first()
        return createTtsSpeakerForModel(context, selectedModelId, downloadManager)
    }

    /**
     * Creates a TTS speaker for a specific model ID.
     * Falls back to system TTS if the model is not available.
     *
     * @param context Application context
     * @param modelId Model ID from TtsModelRegistry, or null for default behavior
     * @param downloadManager Download manager for checking model availability
     * @return TtsSpeakerContract implementation
     */
    suspend fun createTtsSpeakerForModel(
        context: Context,
        modelId: String?,
        downloadManager: TtsModelDownloadManagerContract? = null,
    ): TtsSpeakerContract {
        if (modelId == null) {
            return NoOpTtsSpeaker()
        }
        if (modelId == TtsModelRegistry.SYSTEM_TTS_ID) {
            return SystemTtsSpeaker(context)
        }

        // Get model info
        val modelInfo = TtsModelRegistry.getModelById(modelId)
            ?: return NoOpTtsSpeaker()

        // If it's system TTS, return it directly
        if (modelInfo.isSystemTts) {
            return SystemTtsSpeaker(context)
        }

        // Check if model is downloaded (if download manager is provided)
        val isDownloaded = downloadManager?.isModelDownloaded(modelId) == true

        // If model is not downloaded, stay silent until it becomes available
        if (!isDownloaded) {
            return NoOpTtsSpeaker()
        }

        // Create SherpaOnnx TTS speaker with downloaded model
        val modelPath = downloadManager?.getModelLocalPath(modelId)
            ?: return NoOpTtsSpeaker()

        return createFilesystemTtsSpeaker(context, modelPath, modelInfo.modelFiles)
    }

    /**
     * Creates a TTS speaker using the built-in model (from assets).
     * Legacy method for backward compatibility.
     */
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

    /**
     * Creates a TTS speaker from a filesystem model path.
     * Used for downloaded models.
     */
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

    /**
     * Creates a TTS speaker with fallback logic.
     * Legacy method - prefer using createTtsSpeakerFromPreference.
     */
    fun createTtsSpeakerWithFallback(context: Context, config: TtsConfig = TtsConfig()): TtsSpeakerContract {
        return FallbackTtsSpeaker(context, config)
    }

    /**
     * Checks if a model is ready to use (downloaded or is system TTS).
     */
    suspend fun isModelReady(
        modelId: String,
        downloadManager: TtsModelDownloadManagerContract,
    ): Boolean {
        if (modelId == TtsModelRegistry.SYSTEM_TTS_ID) {
            return true
        }
        return downloadManager.isModelDownloaded(modelId)
    }

    /**
     * Gets the availability status of all models as a Flow.
     */
    fun getModelAvailabilityFlow(
        downloadManager: TtsModelDownloadManagerContract,
    ): Flow<Map<String, Boolean>> {
        return downloadManager.downloadStates.map { states ->
            TtsModelRegistry.getAvailableModels().associate { model ->
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
