package com.hanzi.learner.speech

import android.content.Context
import com.hanzi.learner.data.repository.TtsPreferenceRepositoryContract
import com.hanzi.learner.speech.contract.AudioPlayerContract
import com.hanzi.learner.speech.contract.TtsEngineContract
import com.hanzi.learner.speech.contract.TtsModelDownloadManagerContract
import com.hanzi.learner.speech.contract.TtsSpeakerContract
import com.hanzi.learner.speech.internal.FallbackTtsSpeaker
import com.hanzi.learner.speech.internal.PcmFloatAudioPlayer
import com.hanzi.learner.speech.internal.SherpaOnnxTtsEngine
import com.hanzi.learner.speech.internal.SherpaOnnxTtsSpeaker
import com.hanzi.learner.speech.internal.SystemTtsSpeaker
import com.hanzi.learner.speech.model.TtsModelDownloadState
import com.hanzi.learner.speech.model.TtsModelRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Speech module for creating TTS speakers.
 * Supports both built-in SherpaOnnx models and system TTS.
 */
object SpeechModule {

    data class TtsConfig(
        val modelPath: String = "tts_models/vits-zh-hf-fanchen-wnj.onnx",
        val tokensPath: String = "tts_models/tokens.txt",
        val lexiconPath: String = "tts_models/lexicon.txt",
        val dictDir: String = "tts_models/dict",
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
        // If no model selected or system TTS selected, use system TTS
        if (modelId == null || modelId == TtsModelRegistry.SYSTEM_TTS_ID) {
            return SystemTtsSpeaker(context)
        }

        // Get model info
        val modelInfo = TtsModelRegistry.getModelById(modelId)
            ?: return SystemTtsSpeaker(context) // Fallback to system TTS if model not found

        // If it's system TTS, return it directly
        if (modelInfo.isSystemTts) {
            return SystemTtsSpeaker(context)
        }

        // Check if model is downloaded (if download manager is provided)
        val isDownloaded = downloadManager?.isModelDownloaded(modelId) == true

        // If model is not downloaded, fallback to system TTS
        if (!isDownloaded) {
            return SystemTtsSpeaker(context)
        }

        // Create SherpaOnnx TTS speaker with downloaded model
        val modelPath = downloadManager?.getModelLocalPath(modelId)
            ?: return SystemTtsSpeaker(context)

        return createFilesystemTtsSpeaker(context, modelPath)
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
            context = context,
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
    fun createFilesystemTtsSpeaker(context: Context, modelDirPath: String): TtsSpeakerContract {
        val config = TtsConfig(
            modelPath = "$modelDirPath/model.onnx",
            tokensPath = "$modelDirPath/tokens.txt",
            lexiconPath = "$modelDirPath/lexicon.txt",
            dictDir = "$modelDirPath/dict",
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
