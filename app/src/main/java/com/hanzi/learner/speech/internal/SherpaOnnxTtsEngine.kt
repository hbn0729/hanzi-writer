package com.hanzi.learner.speech.internal

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.hanzi.learner.speech.contract.TtsEngineContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val TAG = "SherpaOnnxTtsEngine"

internal class SherpaOnnxTtsEngine(
    private val context: Context,
    private val modelConfig: TtsModelConfig,
    private val defaultSpeakerId: Int = 0,
    private val defaultSpeed: Float = 1.0f,
) : TtsEngineContract {
    private var tts: OfflineTts? = null
    private var cachedSampleRate: Int = 16000
    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    data class TtsModelConfig(
        val modelPath: String,
        val lexiconPath: String? = null,
        val tokensPath: String? = null,
        val dataDir: String? = null,
        val dictDir: String? = null,
    )

    override suspend fun initialize() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Initializing TTS with assets directly")
                
                val config = buildTtsConfig()
                Log.d(TAG, "Creating OfflineTts with config: $config")
                
                tts = OfflineTts(
                    assetManager = context.assets,
                    config = config
                )
                
                cachedSampleRate = tts?.sampleRate() ?: 16000
                Log.d(TAG, "TTS initialized, sampleRate: $cachedSampleRate")
                
                _isReady.value = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize TTS", e)
            }
        }
    }
    
    private fun buildTtsConfig(): OfflineTtsConfig {
        val vitsConfig = OfflineTtsVitsModelConfig(
            model = modelConfig.modelPath,
            lexicon = modelConfig.lexiconPath ?: "",
            tokens = modelConfig.tokensPath ?: "",
            dataDir = modelConfig.dataDir ?: "",
            dictDir = modelConfig.dictDir ?: "",
            noiseScale = 0.667f,
            noiseScaleW = 0.8f,
            lengthScale = 1.0f,
        )

        val modelCfg = OfflineTtsModelConfig(
            vits = vitsConfig,
            numThreads = 2,
            debug = false,
            provider = "cpu",
        )

        return OfflineTtsConfig(
            model = modelCfg,
            ruleFsts = "",
            ruleFars = "",
            maxNumSentences = 2,
            silenceScale = 0.0f,
        )
    }

    private fun copyAssetsToDir(destDir: File) {
        copyAssetTree(context.assets, "", destDir)
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
            Log.e(TAG, "Error copying asset: $path", e)
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
            Log.d(TAG, "Copied asset: $path")
        } catch (e: IOException) {
            Log.e(TAG, "Error copying asset file: $path", e)
        }
    }

    override suspend fun synthesize(
        text: String,
        speakerId: Int,
        speed: Float,
    ): FloatArray? = withContext(Dispatchers.IO) {
        val actualSpeakerId = if (speakerId < 0) defaultSpeakerId else speakerId
        val actualSpeed = if (speed <= 0) defaultSpeed else speed
        val ttsInstance = tts
        if (ttsInstance == null) {
            Log.e(TAG, "TTS not initialized")
            return@withContext null
        }
        
        try {
            Log.d(TAG, "Synthesizing: '$text', sid: $actualSpeakerId, speed: $actualSpeed")
            val audio = ttsInstance.generate(text = text, sid = actualSpeakerId, speed = actualSpeed)
            Log.d(TAG, "Generated audio: ${audio.samples.size} samples")
            
            if (audio.samples.isNotEmpty()) audio.samples else null
        } catch (e: Exception) {
            Log.e(TAG, "Synthesis failed for: $text", e)
            null
        }
    }

    override fun getSampleRate(): Int = cachedSampleRate

    override fun shutdown() {
        tts?.release()
        tts = null
        _isReady.value = false
    }
}
