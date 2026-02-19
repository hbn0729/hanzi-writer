package com.hanzi.learner.speech.contract

/**
 * Contract for audio playback operations.
 * Follows Interface Segregation Principle - separates playback from synthesis.
 */
interface AudioPlayerContract {
    /**
     * Initialize the audio player with sample rate.
     */
    fun initialize(sampleRate: Int)

    /**
     * Play audio samples.
     */
    fun play(samples: FloatArray)

    /**
     * Stop playback immediately.
     */
    fun stop()

    /**
     * Release resources.
     */
    fun release()
}
