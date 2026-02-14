package com.hanzi.learner.db

data class BackupData(
    val version: Int = 1,
    val progress: List<HanziProgressEntity> = emptyList(),
    val phraseOverrides: List<PhraseOverrideEntity> = emptyList(),
    val disabledChars: List<String> = emptyList(),
    val settings: AppSettingsEntity? = null,
)

