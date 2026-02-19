package com.hanzi.learner.speech

import android.content.Context
import com.hanzi.learner.speech.contract.AudioPlayerContract
import com.hanzi.learner.speech.contract.TtsEngineContract
import com.hanzi.learner.speech.contract.TtsSpeakerContract
import com.hanzi.learner.speech.internal.PcmFloatAudioPlayer
import com.hanzi.learner.speech.internal.SherpaOnnxTtsEngine
import com.hanzi.learner.speech.internal.SherpaOnnxTtsSpeaker

/**
 * Factory for creating TTS components.
 * Centralizes dependency creation for the speech module.
 */
object SpeechModule {

    data class TtsConfig(
        val modelPath: String = "tts_models/vits-zh-hf-fanchen-wnj.onnx",
        val tokensPath: String = "tts_models/tokens.txt",
        val lexiconPath: String = "tts_models/lexicon.txt",
        val dictDir: String = "tts_models/dict",
        val speakerId: Int = 0,
        val speed: Float = 1.0f,
    )

    fun createTtsSpeaker(context: Context, config: TtsConfig = TtsConfig()): TtsSpeakerContract {
        val modelConfig = SherpaOnnxTtsEngine.TtsModelConfig(
            modelPath = config.modelPath,
            tokensPath = config.tokensPath,
            lexiconPath = config.lexiconPath,
            dictDir = config.dictDir,
        )

        val engine: TtsEngineContract = SherpaOnnxTtsEngine(
            context = context,
            modelConfig = modelConfig,
            defaultSpeakerId = config.speakerId,
            defaultSpeed = config.speed,
        )

        val player: AudioPlayerContract = PcmFloatAudioPlayer()

        return SherpaOnnxTtsSpeaker(
            engine = engine,
            player = player,
        )
    }
}
