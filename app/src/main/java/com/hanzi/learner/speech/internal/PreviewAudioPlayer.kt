package com.hanzi.learner.speech.internal

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.hanzi.learner.speech.contract.PreviewAudioPlayerContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale

private const val TAG = "PreviewAudioPlayer"

internal class PreviewAudioPlayer(
    private val context: Context,
) : PreviewAudioPlayerContract {

    private var mediaPlayer: MediaPlayer? = null
    private var systemTts: TextToSpeech? = null
    private var isSystemTtsReady = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var synthesisJob: Job? = null
    private var localAudioPlayer: PcmFloatAudioPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        initializeSystemTts()
    }

    private fun initializeSystemTts() {
        systemTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isSystemTtsReady = true

                val chineseLocales = listOf(
                    Locale.CHINESE,
                    Locale.SIMPLIFIED_CHINESE,
                    Locale("zh", "TW"),
                )

                for (locale in chineseLocales) {
                    val result = systemTts?.isLanguageAvailable(locale)
                        ?: TextToSpeech.LANG_NOT_SUPPORTED
                    if (result >= TextToSpeech.LANG_AVAILABLE) {
                        systemTts?.setLanguage(locale)
                        Log.d(TAG, "Set Chinese locale: $locale (result=$result)")
                        break
                    }
                }

                systemTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isPlaying.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isPlaying.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isPlaying.value = false
                        Log.e(TAG, "TTS error for utterance: $utteranceId")
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isPlaying.value = false
                        Log.e(TAG, "TTS error for utterance: $utteranceId, code: $errorCode")
                    }
                })
            } else {
                Log.e(TAG, "System TTS initialization failed")
            }
        }
    }

    override fun playFromUrl(url: String) {
        playWithDataSource(
            sourceDescription = "URL: $url",
            setDataSource = { it.setDataSource(url) },
        )
    }

    override fun playFromFile(path: String) {
        playWithDataSource(
            sourceDescription = "file: $path",
            setDataSource = { it.setDataSource(path) },
        )
    }

    private fun playWithDataSource(
        sourceDescription: String,
        setDataSource: (MediaPlayer) -> Unit,
    ) {
        releaseMediaPlayer()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this)
                setOnPreparedListener {
                    _isPlaying.value = true
                    start()
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    true
                }
                prepareAsync()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to play from $sourceDescription", e)
            _isPlaying.value = false
        }
    }

    override fun playSystemTtsPreview(text: String) {
        if (!isSystemTtsReady || systemTts == null) {
            Log.w(TAG, "System TTS not ready, cannot play preview")
            return
        }

        stop()
        val utteranceId = "preview_${System.currentTimeMillis()}"
        systemTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun playFromLocalModel(modelDirPath: String, modelFiles: List<String>, text: String) {
        Log.d(TAG, "playFromLocalModel: path=$modelDirPath, files=$modelFiles, text=$text")
        
        val modelDir = File(modelDirPath)
        if (!modelDir.exists() || !modelDir.isDirectory) {
            Log.e(TAG, "Model directory does not exist: $modelDirPath")
            return
        }

        stop()
        synthesisJob?.cancel()

        synthesisJob = scope.launch {
            try {
                _isPlaying.value = true
                
                val samples = withContext(Dispatchers.IO) {
                    synthesizeWithLocalModel(modelDirPath, modelFiles, text)
                }

                if (samples != null && samples.isNotEmpty()) {
                    Log.d(TAG, "Synthesis complete, playing ${samples.size} samples")
                    playLocalSamples(samples)
                } else {
                    Log.e(TAG, "Synthesis returned null or empty samples")
                    _isPlaying.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to synthesize with local model", e)
                _isPlaying.value = false
            }
        }
    }

    private suspend fun synthesizeWithLocalModel(
        modelDirPath: String,
        modelFiles: List<String>,
        text: String,
    ): FloatArray? = withContext(Dispatchers.IO) {
        try {
            val onnxFile = modelFiles.firstOrNull { it.endsWith(".onnx") } ?: "model.onnx"
            val tokensFile = modelFiles.firstOrNull { it.endsWith("tokens.txt") } ?: "tokens.txt"
            val lexiconFile = modelFiles.firstOrNull { it.endsWith("lexicon.txt") } ?: "lexicon.txt"
            val dictDir = File(modelDirPath, "dict").takeIf { it.exists() && it.isDirectory }?.absolutePath.orEmpty()

            val config = SherpaOnnxTtsEngine.TtsModelConfig(
                modelPath = "$modelDirPath/$onnxFile",
                tokensPath = "$modelDirPath/$tokensFile",
                lexiconPath = "$modelDirPath/$lexiconFile",
                dictDir = dictDir,
                useFilesystem = true,
            )

            val engine = SherpaOnnxTtsEngine(
                assetManager = null,
                modelConfig = config,
            )
            
            engine.initialize()
            
            if (!engine.isReady.value) {
                Log.e(TAG, "Engine failed to initialize")
                engine.shutdown()
                return@withContext null
            }

            val samples = engine.synthesize(text)
            engine.shutdown()
            
            samples
        } catch (e: Exception) {
            Log.e(TAG, "Error in synthesizeWithLocalModel", e)
            null
        }
    }

    private fun playLocalSamples(samples: FloatArray) {
        localAudioPlayer?.release()
        localAudioPlayer = PcmFloatAudioPlayer()
        localAudioPlayer?.initialize(16000)
        localAudioPlayer?.play(samples)
    }

    override fun stop() {
        synthesisJob?.cancel()
        synthesisJob = null
        
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            _isPlaying.value = false
        }
        systemTts?.stop()
        
        localAudioPlayer?.stop()
    }

    override fun release() {
        scope.cancel()
        synthesisJob?.cancel()
        synthesisJob = null
        
        releaseMediaPlayer()
        
        localAudioPlayer?.release()
        localAudioPlayer = null
        
        systemTts?.shutdown()
        systemTts = null
        isSystemTtsReady = false
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}