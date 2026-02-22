package com.hanzi.learner.speech.model

/**
 * Singleton registry of available TTS models.
 * Contains hardcoded model metadata for SherpaOnnx models and system TTS option.
 */
object TtsModelRegistry {

    /**
     * Special ID for the system TTS option.
     */
    const val SYSTEM_TTS_ID = "system-tts"

    private val models: List<TtsModelInfo> = listOf(
        TtsModelInfo(
            id = SYSTEM_TTS_ID,
            name = "系统语音",
            description = "使用 Android 系统内置的 TTS 引擎（无需下载）",
            downloadUrl = null,
            previewAudioUrl = "",
            fileSizeBytes = 0,
            modelFiles = emptyList(),
            isSystemTts = true,
        ),
        TtsModelInfo(
            id = "vits-zh-hf-fanchen-wnj",
            name = "中文女声 (VITS)",
            description = "基于 VITS 的高质量中文女声模型，发音清晰自然",
            downloadUrl = "https://hf-mirror.com/csukuangfj/vits-zh-hf-fanchen-wnj/resolve/main",
            previewAudioUrl = "",
            fileSizeBytes = 54_000_000,
            modelFiles = listOf(
                "vits-zh-hf-fanchen-wnj.onnx",
                "tokens.txt",
                "lexicon.txt",
            ),
        ),
        TtsModelInfo(
            id = "vits-zh-hf-theresa",
            name = "中文女声 Theresa (VITS)",
            description = "Theresa 中文女声模型，音色温暖亲切",
            downloadUrl = "https://hf-mirror.com/csukuangfj/vits-zh-hf-theresa/resolve/main",
            previewAudioUrl = "",
            fileSizeBytes = 52_000_000,
            modelFiles = listOf(
                "vits-zh-hf-theresa.onnx",
                "tokens.txt",
                "lexicon.txt",
            ),
        ),
    )

    private val modelsById: Map<String, TtsModelInfo> = models.associateBy { it.id }

    /**
     * Returns all available TTS models including system TTS and SherpaOnnx models.
     */
    fun getAvailableModels(): List<TtsModelInfo> = models

    /**
     * Returns a specific model by its ID, or null if not found.
     */
    fun getModelById(id: String): TtsModelInfo? = modelsById[id]

    /**
     * Returns true if the given ID corresponds to the system TTS option.
     */
    fun isSystemTts(id: String): Boolean = id == SYSTEM_TTS_ID
}