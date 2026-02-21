package com.hanzi.learner.speech.contract

import com.hanzi.learner.speech.model.TtsModelDownloadState
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for TTS model download manager.
 * Manages downloading, pausing, and tracking progress of TTS model files.
 */
interface TtsModelDownloadManagerContract {

    /**
     * Current download state for all models.
     * Maps modelId to its download state.
     */
    val downloadStates: StateFlow<Map<String, TtsModelDownloadState>>

    /**
     * Start downloading a model.
     * @param modelId The unique identifier of the model to download
     * @return true if download was initiated, false if already downloading or downloaded
     */
    suspend fun startDownload(modelId: String): Boolean

    /**
     * Pause an ongoing download.
     * @param modelId The unique identifier of the model to pause
     */
    suspend fun pauseDownload(modelId: String)

    /**
     * Resume a paused download.
     * @param modelId The unique identifier of the model to resume
     */
    suspend fun resumeDownload(modelId: String)

    /**
     * Cancel and remove an ongoing or completed download.
     * @param modelId The unique identifier of the model to cancel
     */
    suspend fun cancelDownload(modelId: String)

    /**
     * Check if a model is fully downloaded and available locally.
     * @param modelId The unique identifier of the model to check
     * @return true if all model files exist locally
     */
    suspend fun isModelDownloaded(modelId: String): Boolean

    /**
     * Get the local directory path for a downloaded model.
     * @param modelId The unique identifier of the model
     * @return Absolute path to the model's directory, or null if not downloaded
     */
    fun getModelLocalPath(modelId: String): String?

    /**
     * Clean up resources and cancel all active downloads.
     */
    fun release()
}
