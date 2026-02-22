package com.hanzi.learner.speech.contract

import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for preview audio playback operations.
 * Used for playing pre-recorded model preview audio and system TTS previews.
 */
interface PreviewAudioPlayerContract {
    /**
     * StateFlow indicating whether audio is currently playing.
     */
    val isPlaying: StateFlow<Boolean>

    /**
     * Play audio from a remote URL.
     * @param url The URL of the audio file to play
     */
    fun playFromUrl(url: String)

    /**
     * Play audio from a local file path.
     * @param path The absolute path to the audio file
     */
    fun playFromFile(path: String)

    /**
     * Play a preview using system TTS.
     * @param text The text to synthesize and play
     */
    fun playSystemTtsPreview(text: String)

    /**
     * Play a preview using a locally downloaded TTS model.
     * @param modelDirPath Absolute path to the model directory
     * @param modelFiles List of model file names (e.g., ["model.onnx", "tokens.txt"])
     * @param text The text to synthesize and play
     */
    fun playFromLocalModel(modelDirPath: String, modelFiles: List<String>, text: String)

    /**
     * Stop the current playback.
     */
    fun stop()

    /**
     * Release all resources. Should be called when done using the player.
     */
    fun release()
}