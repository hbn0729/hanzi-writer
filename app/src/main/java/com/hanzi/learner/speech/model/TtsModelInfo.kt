package com.hanzi.learner.speech.model

/**
 * Immutable data class representing a TTS model's metadata.
 *
 * @property id Unique identifier for the model (e.g., "vits-zh-hf-fanchen-wnj")
 * @property name Human-readable display name
 * @property description Brief description of the model's characteristics
 * @property downloadUrl URL to download the model files (null for system TTS)
 * @property previewAudioUrl URL to the pre-recorded preview audio file
 * @property fileSizeBytes Total size of all model files in bytes
 * @property modelFiles List of file names that constitute this model (e.g., ["model.onnx", "tokens.txt"])
 * @property isSystemTts True if this represents the system TTS option (not a downloadable model)
 */
data class TtsModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val downloadUrl: String?,
    val previewAudioUrl: String,
    val fileSizeBytes: Long,
    val modelFiles: List<String>,
    val isSystemTts: Boolean = false,
)