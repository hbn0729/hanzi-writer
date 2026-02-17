package com.hanzi.learner.data.model

import com.hanzi.learner.data.local.entity.AppSettingsEntity
import com.hanzi.learner.data.local.entity.HanziProgressEntity
import com.hanzi.learner.data.local.entity.PhraseOverrideEntity

data class BackupData(
    val version: Int = 1,
    val progress: List<HanziProgressEntity> = emptyList(),
    val phraseOverrides: List<PhraseOverrideEntity> = emptyList(),
    val disabledChars: List<String> = emptyList(),
    val settings: AppSettingsEntity? = null,
)

