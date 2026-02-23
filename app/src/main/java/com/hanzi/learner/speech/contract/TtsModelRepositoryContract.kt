package com.hanzi.learner.speech.contract

import com.hanzi.learner.speech.model.TtsModelInfo

interface TtsModelRepositoryContract {
    fun getAvailableModels(): List<TtsModelInfo>

    fun getModelById(id: String): TtsModelInfo?

    fun isSystemTts(id: String): Boolean

    companion object {
        const val SYSTEM_TTS_ID = "system-tts"
        const val PREVIEW_TEXT = "这是一条试听朗读"
    }
}
