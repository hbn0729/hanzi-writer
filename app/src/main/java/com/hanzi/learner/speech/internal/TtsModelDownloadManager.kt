package com.hanzi.learner.speech.internal

import com.hanzi.learner.speech.contract.TtsModelDownloadManagerContract
import com.hanzi.learner.speech.contract.TtsModelRepositoryContract
import com.hanzi.learner.speech.model.TtsModelDownloadState
import com.hanzi.learner.speech.model.TtsModelRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class TtsModelDownloadManager(
    private val modelsBaseDir: File,
    private val modelRepository: TtsModelRepositoryContract = TtsModelRegistry(),
) : TtsModelDownloadManagerContract {

    companion object {
        private const val BUFFER_SIZE = 8192
        private const val CONNECT_TIMEOUT_MS = 30000
        private const val READ_TIMEOUT_MS = 30000
    }

    private val _downloadStates = MutableStateFlow<Map<String, TtsModelDownloadState>>(emptyMap())
    override val downloadStates: StateFlow<Map<String, TtsModelDownloadState>> = _downloadStates.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeDownloads = ConcurrentHashMap<String, Job>()
    private val pauseSignals = ConcurrentHashMap<String, MutableStateFlow<Boolean>>()
    private val mutex = Mutex()

    init {
        val initialStates = mutableMapOf<String, TtsModelDownloadState>()
        modelRepository.getAvailableModels()
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
        val model = modelRepository.getModelById(modelId) ?: return false
        if (model.isSystemTts || model.downloadUrl == null) return false

        val currentState = _downloadStates.value[modelId]
        if (currentState is TtsModelDownloadState.Downloading ||
            currentState is TtsModelDownloadState.Downloaded
        ) {
            return false
        }

        if (currentState !is TtsModelDownloadState.Paused) {
            deleteModelFiles(modelId)
        }

        val pauseSignal = MutableStateFlow(false)
        pauseSignals[modelId] = pauseSignal

        updateState(modelId) {
            TtsModelDownloadState.Downloading(
                progress = 0f,
                bytesDownloaded = 0L,
                totalBytes = model.fileSizeBytes,
            )
        }

        val job = scope.launch {
            performDownload(modelId)
        }
        activeDownloads[modelId] = job

        return true
    }

    override suspend fun pauseDownload(modelId: String) {
        pauseSignals[modelId]?.value = true

        val model = modelRepository.getModelById(modelId) ?: return
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
        val model = modelRepository.getModelById(modelId) ?: return
        val currentState = _downloadStates.value[modelId]

        if (currentState !is TtsModelDownloadState.Paused) return

        pauseSignals[modelId]?.value = false

        updateState(modelId) {
            TtsModelDownloadState.Downloading(
                progress = currentState.progress,
                bytesDownloaded = currentState.bytesDownloaded,
                totalBytes = currentState.totalBytes,
            )
        }
    }

    override suspend fun cancelDownload(modelId: String) {
        activeDownloads[modelId]?.cancel()
        activeDownloads.remove(modelId)

        deleteModelFiles(modelId)
        pauseSignals.remove(modelId)

        updateState(modelId) { TtsModelDownloadState.NotDownloaded }
    }

    override suspend fun isModelDownloaded(modelId: String): Boolean {
        return isModelFilesExist(modelId)
    }

    override fun getModelLocalPath(modelId: String): String? {
        val dir = getModelDirectory(modelId)
        return if (isModelFilesExist(modelId)) dir.absolutePath else null
    }

    override fun release() {
        activeDownloads.values.forEach { it.cancel() }
        activeDownloads.clear()
        pauseSignals.clear()
        scope.cancel()
    }

    suspend fun performDownload(modelId: String) {
        val model = modelRepository.getModelById(modelId) ?: return
        val downloadUrlBase = model.downloadUrl ?: return

        withContext(Dispatchers.IO) {
            try {
                val modelDir = getModelDirectory(modelId)
                modelDir.mkdirs()

                val requiredFiles = model.modelFiles
                if (requiredFiles.isEmpty()) {
                    throw IllegalStateException("Model has no modelFiles: $modelId")
                }

                val pauseSignal = pauseSignals[modelId] ?: MutableStateFlow(false)

                supervisorScope {
                    for (fileName in requiredFiles) {
                        downloadSingleFile(
                            modelId = modelId,
                            downloadUrlBase = downloadUrlBase,
                            modelDir = modelDir,
                            fileName = fileName,
                            pauseSignal = pauseSignal,
                            totalBytesExpected = model.fileSizeBytes,
                        )
                    }
                }

                if (!isModelFilesExist(modelId)) {
                    throw IOException("Downloaded but required files missing for $modelId")
                }

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
        val model = modelRepository.getModelById(modelId) ?: return false
        val requiredFiles = model.modelFiles
        if (requiredFiles.isEmpty()) return false

        val modelDir = getModelDirectory(modelId)
        if (!modelDir.exists() || !modelDir.isDirectory) return false

        return requiredFiles.all { fileName ->
            val f = File(modelDir, fileName)
            f.exists() && f.isFile && f.length() > 0L
        }
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

    private suspend fun downloadSingleFile(
        modelId: String,
        downloadUrlBase: String,
        modelDir: File,
        fileName: String,
        pauseSignal: MutableStateFlow<Boolean>,
        totalBytesExpected: Long,
    ) {
        val urlString = if (downloadUrlBase.endsWith("/")) {
            "$downloadUrlBase$fileName"
        } else {
            "$downloadUrlBase/$fileName"
        }

        val outputFile = File(modelDir, fileName)
        val existingBytes = if (outputFile.exists()) outputFile.length() else 0L

        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("Accept-Encoding", "identity")
            if (existingBytes > 0) {
                setRequestProperty("Range", "bytes=$existingBytes-")
            }
        }

        connection.connect()

        if (connection.responseCode !in 200..299) {
            throw IOException("HTTP ${connection.responseCode}: ${connection.responseMessage}")
        }

        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        var fileBytesRead = existingBytes

        connection.inputStream.use { input ->
            val outputStream = if (existingBytes > 0) {
                outputFile.outputStream().apply { channel.position(existingBytes) }
            } else {
                outputFile.outputStream()
            }

            outputStream.use { output ->
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    if (!currentCoroutineContext().isActive) {
                        throw CancellationException("Download cancelled")
                    }

                    if (pauseSignal.value) {
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

                        while (pauseSignal.value && currentCoroutineContext().isActive) {
                            kotlinx.coroutines.delay(100)
                        }
                        if (!currentCoroutineContext().isActive) {
                            throw CancellationException("Download cancelled while paused")
                        }
                    }

                    output.write(buffer, 0, bytesRead)
                    fileBytesRead += bytesRead

                    val totalDownloaded = getModelTotalDownloadedBytes(modelId)
                    val progress = if (totalBytesExpected > 0) {
                        (totalDownloaded.toFloat() / totalBytesExpected).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    updateState(modelId) {
                        TtsModelDownloadState.Downloading(
                            progress = progress,
                            bytesDownloaded = totalDownloaded,
                            totalBytes = totalBytesExpected,
                        )
                    }
                }
            }
        }
    }

    private fun getModelTotalDownloadedBytes(modelId: String): Long {
        val model = modelRepository.getModelById(modelId) ?: return 0L
        val modelDir = getModelDirectory(modelId)
        return model.modelFiles.sumOf { fileName ->
            val f = File(modelDir, fileName)
            if (f.exists() && f.isFile) f.length() else 0L
        }.coerceAtMost(Long.MAX_VALUE)
    }
}
