package com.hanzi.learner.speech.model

/**
 * Represents the current download state of a TTS model.
 */
sealed class TtsModelDownloadState {
    /**
     * Model is not downloaded and not being downloaded.
     */
    object NotDownloaded : TtsModelDownloadState()

    /**
     * Model is currently being downloaded.
     * @property progress Download progress from 0.0 to 1.0
     * @property bytesDownloaded Number of bytes downloaded so far
     * @property totalBytes Total size of the model in bytes
     */
    data class Downloading(
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long,
    ) : TtsModelDownloadState()

    /**
     * Download is paused.
     * @property progress Download progress when paused (0.0 to 1.0)
     * @property bytesDownloaded Number of bytes downloaded so far
     * @property totalBytes Total size of the model in bytes
     */
    data class Paused(
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long,
    ) : TtsModelDownloadState()

    /**
     * Model is fully downloaded and available locally.
     * @property localPath Absolute path to the model directory
     */
    data class Downloaded(
        val localPath: String,
    ) : TtsModelDownloadState()

    /**
     * Download failed.
     * @property error Error message
     * @param retryable Whether the download can be retried
     */
    data class Error(
        val error: String,
        val retryable: Boolean = true,
    ) : TtsModelDownloadState()
}
