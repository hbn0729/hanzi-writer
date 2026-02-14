package com.hanzi.learner.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TtsSpeaker(
    context: Context,
) : TtsSpeakerContract, TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    
    private val _isReady = MutableStateFlow(false)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    
    private var pendingCharacter: String? = null
    private var pendingPhrase: String? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.SIMPLIFIED_CHINESE
            _isReady.value = true
            
            pendingCharacter?.let { char ->
                speakCharacterAndPhraseInternal(char, pendingPhrase.orEmpty())
                pendingCharacter = null
                pendingPhrase = null
            }
        }
    }

    override fun speak(text: String) {
        if (!_isReady.value) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    override fun speakCharacterAndPhrase(character: String, phrase: String) {
        if (!_isReady.value) {
            pendingCharacter = character
            pendingPhrase = phrase
            return
        }
        speakCharacterAndPhraseInternal(character, phrase)
    }
    
    private fun speakCharacterAndPhraseInternal(character: String, phrase: String) {
        tts.speak(character, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        if (phrase.isNotEmpty()) {
            tts.speak(phrase, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
        }
    }

    override fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
