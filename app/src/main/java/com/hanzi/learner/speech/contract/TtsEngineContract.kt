package com.hanzi.learner.speech.contract

import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for TTS Engine operations.
 * Follows Interface Segregation Principle - focused on TTS synthesis only.
 */
interface TtsEngineContract {
    /**
     * Emits true when TTS engine is initialized and ready.
     */
    val isReady: StateFlow<Boolean>

    /**
     * Initialize the TTS engine with model configuration.
     * Should be called before any synthesis operations.
     */
    suspend fun initialize()

    /**
     * Synthesize text into audio samples.
     * @param text The text to synthesize
     * @param speakerId Speaker ID (for multi-speaker models)
     * @param speed Speech speed factor (1.0 = normal)
     * @return Audio samples as FloatArray, or null if synthesis failed
     */
    suspend fun synthesize(
        text: String,
        speakerId: Int = 0,
        speed: Float = 1.0f,
    ): FloatArray?

    /**
     * Get the sample rate of generated audio.
     */
    fun getSampleRate(): Int

    /**
     * Release resources and shutdown the engine.
     */
    fun shutdown()
}
