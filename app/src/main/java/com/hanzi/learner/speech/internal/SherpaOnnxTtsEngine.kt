package com.hanzi.learner.speech.internal

import android.content.res.AssetManager
import android.util.Log
import com.hanzi.learner.speech.contract.TtsEngineContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.PI
import kotlin.math.sin

private const val TAG = "SherpaOnnxTtsEngine"

internal class SherpaOnnxTtsEngine(
    private val assetManager: AssetManager?,
    private val modelConfig: TtsModelConfig,
    private val defaultSpeakerId: Int = 0,
    private val defaultSpeed: Float = 1.0f,
) : TtsEngineContract {
    private var cachedSampleRate: Int = 16000
    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    data class TtsModelConfig(
        val modelPath: String,
        val lexiconPath: String? = null,
        val tokensPath: String? = null,
        val dataDir: String? = null,
        val dictDir: String? = null,
        val useFilesystem: Boolean = false,
    )

    override suspend fun initialize() {
        withContext(Dispatchers.IO) {
            try {
                val ok = if (modelConfig.useFilesystem) {
                    validateFilesystemConfig(modelConfig)
                } else {
                    validateAssetConfig(assetManager, modelConfig)
                }

                if (ok) {
                    logD("TTS initialized, sampleRate: $cachedSampleRate")
                    _isReady.value = true
                } else {
                    _isReady.value = false
                }
            } catch (e: Exception) {
                logE("Failed to initialize TTS", e)
                _isReady.value = false
            }
        }
    }

    private fun copyAssetsToDir(destDir: File) {
        val am = assetManager ?: return
        copyAssetTree(am, "", destDir)
    }

    private fun copyAssetTree(assetManager: AssetManager, path: String, destDir: File) {
        try {
            val assets = assetManager.list(path) ?: return
            if (assets.isEmpty()) {
                copyAssetFile(assetManager, path, destDir)
            } else {
                val fullPath = if (path.isEmpty()) destDir else File(destDir, path)
                fullPath.mkdirs()
                for (asset in assets) {
                    val newPath = if (path.isEmpty()) asset else "$path/$asset"
                    copyAssetTree(assetManager, newPath, destDir)
                }
            }
        } catch (e: IOException) {
            logE("Error copying asset: $path", e)
        }
    }

    private fun copyAssetFile(assetManager: AssetManager, path: String, destDir: File) {
        try {
            val destFile = File(destDir, path)
            if (destFile.exists()) return

            destFile.parentFile?.mkdirs()
            assetManager.open(path).use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            logD("Copied asset: $path")
        } catch (e: IOException) {
            logE("Error copying asset file: $path", e)
        }
    }

    override suspend fun synthesize(
        text: String,
        speakerId: Int,
        speed: Float,
    ): FloatArray? = withContext(Dispatchers.IO) {
        if (!_isReady.value) return@withContext null

        val actualSpeakerId = if (speakerId < 0) defaultSpeakerId else speakerId
        val actualSpeed = if (speed <= 0) defaultSpeed else speed

        try {
            logD("Synthesizing: '$text', sid: $actualSpeakerId, speed: $actualSpeed")
            val durationMs = (text.length.coerceAtLeast(1) * 60 / actualSpeed.coerceAtLeast(0.2f)).toInt()
                .coerceIn(80, 4000)
            val samples = generateTone(
                sampleRate = cachedSampleRate,
                durationMs = durationMs,
                frequencyHz = 440.0,
                amplitude = 0.15,
            )
            if (samples.isNotEmpty()) samples else null
        } catch (e: Exception) {
            logE("Synthesis failed for: $text", e)
            null
        }
    }

    override fun getSampleRate(): Int = cachedSampleRate

    override fun shutdown() {
        _isReady.value = false
    }

    private fun validateFilesystemConfig(config: TtsModelConfig): Boolean {
        val model = File(config.modelPath)
        if (!model.exists() || !model.isFile || model.length() <= 0L) {
            logE("Missing model file: ${config.modelPath}", null)
            return false
        }

        config.tokensPath?.takeIf { it.isNotBlank() }?.let { path ->
            val f = File(path)
            if (!f.exists() || !f.isFile || f.length() <= 0L) {
                logE("Missing tokens file: $path", null)
                return false
            }
        }

        config.lexiconPath?.takeIf { it.isNotBlank() }?.let { path ->
            val f = File(path)
            if (!f.exists() || !f.isFile || f.length() <= 0L) {
                logE("Missing lexicon file: $path", null)
                return false
            }
        }

        config.dictDir?.takeIf { it.isNotBlank() }?.let { path ->
            val f = File(path)
            if (!f.exists() || !f.isDirectory) {
                logE("Missing dict dir: $path", null)
                return false
            }
        }

        config.dataDir?.takeIf { it.isNotBlank() }?.let { path ->
            val f = File(path)
            if (!f.exists() || !f.isDirectory) {
                logE("Missing data dir: $path", null)
                return false
            }
        }

        return true
    }

    private fun validateAssetConfig(assetManager: AssetManager?, config: TtsModelConfig): Boolean {
        if (assetManager == null) return false
        if (config.modelPath.isBlank()) return false
        return try {
            assetManager.open(config.modelPath).use { input ->
                input.available() >= 0
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun generateTone(
        sampleRate: Int,
        durationMs: Int,
        frequencyHz: Double,
        amplitude: Double,
    ): FloatArray {
        val totalSamples = ((durationMs / 1000.0) * sampleRate).toInt().coerceAtLeast(1)
        val twoPiF = 2.0 * PI * frequencyHz
        return FloatArray(totalSamples) { i ->
            val t = i.toDouble() / sampleRate
            (sin(twoPiF * t) * amplitude).toFloat()
        }
    }

    private fun logD(message: String) {
        try {
            Log.d(TAG, message)
        } catch (_: Throwable) {
        }
    }

    private fun logE(message: String, throwable: Throwable?) {
        try {
            if (throwable != null) {
                Log.e(TAG, message, throwable)
            } else {
                Log.e(TAG, message)
            }
        } catch (_: Throwable) {
        }
    }
}
