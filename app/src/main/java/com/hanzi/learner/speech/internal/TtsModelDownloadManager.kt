package com.hanzi.learner.speech.internal

import android.content.Context
import com.hanzi.learner.speech.contract.TtsModelDownloadManagerContract
import com.hanzi.learner.speech.model.TtsModelDownloadState
import com.hanzi.learner.speech.model.TtsModelRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of TTS model download manager.
 * Downloads model files using HttpURLConnection with support for progress tracking,
 * pause/resume, and cancellation.
 */
class TtsModelDownloadManager(
    private val context: Context,
    private val modelRegistry: TtsModelRegistry = TtsModelRegistry,
) : TtsModelDownloadManagerContract {

    companion object {
        private const val BUFFER_SIZE = 8192
        private const val CONNECT_TIMEOUT_MS = 30000
        private const val READ_TIMEOUT_MS = 30000
        private const val MODELS_DIR = "tts_models"
    }

    private val _downloadStates = MutableStateFlow<Map<String, TtsModelDownloadState>>(emptyMap())
    override val downloadStates: StateFlow<Map<String, TtsModelDownloadState>> = _downloadStates.asStateFlow()

    private val activeDownloads = ConcurrentHashMap<String, Job>()
    private val downloadProgress = ConcurrentHashMap<String, Long>() // bytes downloaded
    private val pauseSignals = ConcurrentHashMap<String, MutableStateFlow<Boolean>>()
    private val mutex = Mutex()

    private val modelsBaseDir: File
        get() = File(context.filesDir, MODELS_DIR)

    init {
        // Initialize states for all downloadable models
        val initialStates = mutableMapOf<String, TtsModelDownloadState>()
        modelRegistry.getAvailableModels()
            .filter { !it.isSystemTts }
            .forEach { model ->
                initialStates[model.id] = if (isModelFilesExist(model.id)) {
                    TtsModelDownloadState.Downloaded(getModelDirectory(model.id).absolutePath)
                } else {
                    TtsModelDownloadState.NotDownloaded
                }
            }
        _downloadStates.value = initialStates
    }

    override suspend fun startDownload(modelId: String): Boolean = mutex.withLock {
        val model = modelRegistry.getModelById(modelId) ?: return false
        if (model.isSystemTts || model.downloadUrl == null) return false

        val currentState = _downloadStates.value[modelId]
        if (currentState is TtsModelDownloadState.Downloading ||
            currentState is TtsModelDownloadState.Downloaded
        ) {
            return false
        }

        // Clean up any existing files for fresh download
        if (currentState !is TtsModelDownloadState.Paused) {
            deleteModelFiles(modelId)
            downloadProgress[modelId] = 0L
        }

        // Create pause signal
        val pauseSignal = MutableStateFlow(false)
        pauseSignals[modelId] = pauseSignal

        // Update state to downloading
        updateState(modelId) {
            val progress = downloadProgress[modelId] ?: 0L
            TtsModelDownloadState.Downloading(
                progress = progress.toFloat() / model.fileSizeBytes,
                bytesDownloaded = progress,
                totalBytes = model.fileSizeBytes,
            )
        }

        return true
    }

    override suspend fun pauseDownload(modelId: String) {
        pauseSignals[modelId]?.value = true

        val model = modelRegistry.getModelById(modelId) ?: return
        updateState(modelId) { currentState ->
            if (currentState is TtsModelDownloadState.Downloading) {
                TtsModelDownloadState.Paused(
                    progress = currentState.progress,
                    bytesDownloaded = currentState.bytesDownloaded,
                    totalBytes = currentState.totalBytes,
                )
            } else {
                currentState
            }
        }
    }

    override suspend fun resumeDownload(modelId: String) {
        val model = modelRegistry.getModelById(modelId) ?: return
        val currentState = _downloadStates.value[modelId]

        if (currentState !is TtsModelDownloadState.Paused) return

        // Reset pause signal
        pauseSignals[modelId]?.value = false

        // Update state back to downloading
        updateState(modelId) {
            TtsModelDownloadState.Downloading(
                progress = currentState.progress,
                bytesDownloaded = currentState.bytesDownloaded,
                totalBytes = currentState.totalBytes,
            )
        }
    }

    override suspend fun cancelDownload(modelId: String) {
        // Cancel active job
        activeDownloads[modelId]?.cancel()
        activeDownloads.remove(modelId)

        // Delete downloaded files
        deleteModelFiles(modelId)
        downloadProgress.remove(modelId)
        pauseSignals.remove(modelId)

        // Update state
        updateState(modelId) { TtsModelDownloadState.NotDownloaded }
    }

    override suspend fun isModelDownloaded(modelId: String): Boolean {
        return isModelFilesExist(modelId)
    }

    override fun getModelLocalPath(modelId: String): String? {
        val dir = getModelDirectory(modelId)
        return if (dir.exists() && dir.isDirectory) dir.absolutePath else null
    }

    override fun release() {
        activeDownloads.values.forEach { it.cancel() }
        activeDownloads.clear()
        pauseSignals.clear()
    }

    /**
     * Internal method to perform the actual download.
     * Should be called from a coroutine.
     */
    suspend fun performDownload(modelId: String) {
        val model = modelRegistry.getModelById(modelId) ?: return
        val downloadUrl = model.downloadUrl ?: return

        withContext(Dispatchers.IO) {
            try {
                val modelDir = getModelDirectory(modelId)
                modelDir.mkdirs()

                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    requestMethod = "GET"
                    setRequestProperty("Accept-Encoding", "identity")
                }

                // Support resume
                val existingBytes = downloadProgress[modelId] ?: 0L
                if (existingBytes > 0) {
                    connection.setRequestProperty("Range", "bytes=$existingBytes-")
                }

                connection.connect()

                if (connection.responseCode !in 200..299) {
                    throw IOException("HTTP ${connection.responseCode}: ${connection.responseMessage}")
                }

                val inputStream = connection.inputStream
                val outputFile = File(modelDir, "model.zip")
                val outputStream = if (existingBytes > 0) {
                    outputFile.outputStream().apply { channel.position(existingBytes) }
                } else {
                    outputFile.outputStream()
                }

                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                var totalBytesRead = existingBytes
                val pauseSignal = pauseSignals[modelId] ?: MutableStateFlow(false)

                inputStream.use { input ->
                    outputStream.use { output ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            // Check for cancellation
                            if (!currentCoroutineContext().isActive) {
                                throw CancellationException("Download cancelled")
                            }

                            // Check for pause
                            if (pauseSignal.value) {
                                // Save progress and wait
                                downloadProgress[modelId] = totalBytesRead
                                while (pauseSignal.value && currentCoroutineContext().isActive) {
                                    kotlinx.coroutines.delay(100)
                                }
                                if (!currentCoroutineContext().isActive) {
                                    throw CancellationException("Download cancelled while paused")
                                }
                            }

                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            // Update progress
                            val progress = totalBytesRead.toFloat() / model.fileSizeBytes
                            updateState(modelId) {
                                TtsModelDownloadState.Downloading(
                                    progress = progress.coerceIn(0f, 1f),
                                    bytesDownloaded = totalBytesRead,
                                    totalBytes = model.fileSizeBytes,
                                )
                            }
                        }
                    }
                }

                // Download complete - extract if needed or mark as complete
                // For now, assume single file download
                downloadProgress[modelId] = totalBytesRead

                updateState(modelId) {
                    TtsModelDownloadState.Downloaded(modelDir.absolutePath)
                }

                activeDownloads.remove(modelId)
                pauseSignals.remove(modelId)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateState(modelId) {
                    TtsModelDownloadState.Error(
                        error = e.message ?: "Unknown error",
                        retryable = true,
                    )
                }
                activeDownloads.remove(modelId)
            }
        }
    }

    private fun getModelDirectory(modelId: String): File {
        return File(modelsBaseDir, modelId)
    }

    private fun isModelFilesExist(modelId: String): Boolean {
        val modelDir = getModelDirectory(modelId)
        return modelDir.exists() && modelDir.isDirectory && modelDir.listFiles()?.isNotEmpty() == true
    }

    private fun deleteModelFiles(modelId: String) {
        val modelDir = getModelDirectory(modelId)
        if (modelDir.exists()) {
            modelDir.deleteRecursively()
        }
    }

    private inline fun updateState(
        modelId: String,
        transform: (TtsModelDownloadState) -> TtsModelDownloadState,
    ) {
        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            val current = get(modelId) ?: TtsModelDownloadState.NotDownloaded
            put(modelId, transform(current))
        }
    }
}
