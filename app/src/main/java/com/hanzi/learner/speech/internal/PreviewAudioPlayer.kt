package com.hanzi.learner.speech.internal

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.hanzi.learner.speech.contract.PreviewAudioPlayerContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.util.Locale

private const val TAG = "PreviewAudioPlayer"

/**
 * Implementation of PreviewAudioPlayerContract using Android MediaPlayer.
 * Supports playing from URL, local file, and system TTS.
 */
internal class PreviewAudioPlayer(
    private val context: Context,
) : PreviewAudioPlayerContract {

    private var mediaPlayer: MediaPlayer? = null
    private var systemTts: TextToSpeech? = null
    private var isSystemTtsReady = false

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        initializeSystemTts()
    }

    private fun initializeSystemTts() {
        systemTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val chineseLocale = Locale.CHINESE
                val result = systemTts?.isLanguageAvailable(chineseLocale)
                isSystemTtsReady = result != null && result >= TextToSpeech.LANG_AVAILABLE

                if (isSystemTtsReady) {
                    systemTts?.setLanguage(chineseLocale)
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
        releaseMediaPlayer()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
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
            Log.e(TAG, "Failed to play from URL: $url", e)
            _isPlaying.value = false
        }
    }

    override fun playFromFile(path: String) {
        releaseMediaPlayer()

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
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
            Log.e(TAG, "Failed to play from file: $path", e)
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

    override fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            _isPlaying.value = false
        }
        systemTts?.stop()
    }

    override fun release() {
        releaseMediaPlayer()
        systemTts?.shutdown()
        systemTts = null
        isSystemTtsReady = false
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}