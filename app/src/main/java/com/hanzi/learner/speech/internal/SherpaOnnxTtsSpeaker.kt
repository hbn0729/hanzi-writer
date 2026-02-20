package com.hanzi.learner.speech.internal

import android.util.Log
import com.hanzi.learner.speech.contract.AudioPlayerContract
import com.hanzi.learner.speech.contract.TtsEngineContract
import com.hanzi.learner.speech.contract.TtsSpeakerContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "SherpaOnnxTtsSpeaker"

internal class SherpaOnnxTtsSpeaker(
    private val engine: TtsEngineContract,
    private val player: AudioPlayerContract,
) : TtsSpeakerContract {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var currentJob: Job? = null
    private var isPlayerReady = false

    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private data class PendingRequest(
        val type: RequestType,
        val text: String = "",
        val character: String = "",
        val phrase: String = "",
    )

    private enum class RequestType {
        SPEAK,
        SPEAK_CHARACTER_AND_PHRASE,
    }

    private var pendingRequest: PendingRequest? = null

    init {
        scope.launch {
            Log.d(TAG, "Initializing TTS engine...")
            engine.initialize()
            Log.d(TAG, "Engine initialized, initializing player with sampleRate: ${engine.getSampleRate()}")
            player.initialize(engine.getSampleRate())
            isPlayerReady = true
            updateReadyState()
            Log.d(TAG, "Player initialized, isReady: ${_isReady.value}")
        }

        scope.launch {
            engine.isReady.collect {
                updateReadyState()
            }
        }
    }

    private fun updateReadyState() {
        val wasReady = _isReady.value
        val nowReady = engine.isReady.value && isPlayerReady
        _isReady.value = nowReady

        if (!wasReady && nowReady) {
            val pending = pendingRequest
            if (pending != null) {
                Log.d(TAG, "TTS is now fully ready, playing pending request: $pending")
                pendingRequest = null
                when (pending.type) {
                    RequestType.SPEAK -> executeSpeak(pending.text)
                    RequestType.SPEAK_CHARACTER_AND_PHRASE -> executeSpeakCharacterAndPhrase(
                        pending.character,
                        pending.phrase
                    )
                }
            }
        }
    }

    override fun speak(text: String) {
        Log.d(TAG, "speak() called: $text, isReady: ${_isReady.value}")
        if (!_isReady.value) {
            Log.w(TAG, "Speaker not ready, queuing speak request")
            pendingRequest = PendingRequest(RequestType.SPEAK, text = text)
            return
        }
        executeSpeak(text)
    }

    private fun executeSpeak(text: String) {
        currentJob?.cancel()
        currentJob = scope.launch {
            val samples = engine.synthesize(text)
            Log.d(TAG, "Synthesized ${samples?.size ?: 0} samples")
            if (samples != null && samples.isNotEmpty()) {
                player.stop()
                player.play(samples)
                Log.d(TAG, "Playback complete")
            }
        }
    }

    override fun speakCharacterAndPhrase(character: String, phrase: String) {
        Log.d(TAG, "speakCharacterAndPhrase() called: '$character', '$phrase', isReady: ${_isReady.value}")
        if (!_isReady.value) {
            Log.w(TAG, "Speaker not ready, queuing speakCharacterAndPhrase request")
            pendingRequest = PendingRequest(
                RequestType.SPEAK_CHARACTER_AND_PHRASE,
                character = character,
                phrase = phrase
            )
            return
        }
        executeSpeakCharacterAndPhrase(character, phrase)
    }

    private fun executeSpeakCharacterAndPhrase(character: String, phrase: String) {
        currentJob?.cancel()
        currentJob = scope.launch {
            player.stop()

            val charSamples = engine.synthesize(character)
            Log.d(TAG, "Char synthesized: ${charSamples?.size ?: 0} samples")
            if (charSamples != null && charSamples.isNotEmpty()) {
                player.play(charSamples)
                Log.d(TAG, "Char playback done")
            }

            if (phrase.isNotEmpty()) {
                delay(500)
                val phraseSamples = engine.synthesize(phrase)
                Log.d(TAG, "Phrase synthesized: ${phraseSamples?.size ?: 0} samples")
                if (phraseSamples != null && phraseSamples.isNotEmpty()) {
                    player.play(phraseSamples)
                    Log.d(TAG, "Phrase playback done")
                }
            }
        }
    }

    override fun shutdown() {
        currentJob?.cancel()
        player.release()
        engine.shutdown()
    }
}
