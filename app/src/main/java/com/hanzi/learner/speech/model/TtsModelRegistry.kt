package com.hanzi.learner.speech.model

import com.hanzi.learner.speech.contract.TtsModelRepositoryContract

class TtsModelRegistry : TtsModelRepositoryContract {

    private val models: List<TtsModelInfo> = listOf(
        TtsModelInfo(
            id = TtsModelRepositoryContract.SYSTEM_TTS_ID,
            name = "系统语音",
            description = "使用 Android 系统内置的 TTS 引擎（无需下载）。建议安装\"科大讯飞语音引擎\"获得更好的中文语音效果。",
            downloadUrl = null,
            previewAudioUrl = "",
            fileSizeBytes = 0,
            modelFiles = emptyList(),
            isSystemTts = true,
        ),
    )

    private val modelsById: Map<String, TtsModelInfo> = models.associateBy { it.id }

    override fun getAvailableModels(): List<TtsModelInfo> = models

    override fun getModelById(id: String): TtsModelInfo? = modelsById[id]

    override fun isSystemTts(id: String): Boolean = id == TtsModelRepositoryContract.SYSTEM_TTS_ID
}
