package com.hanzi.learner.speech.model

data class TtsModelUiItem(
    val info: TtsModelInfo,
    val downloadState: TtsModelDownloadState,
    val isSelected: Boolean,
)

