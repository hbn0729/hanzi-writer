package com.hanzi.learner.features.admin.repository

import com.hanzi.learner.data.local.entity.AppSettingsEntity
import com.hanzi.learner.data.local.entity.HanziProgressEntity
import com.hanzi.learner.data.local.entity.PhraseOverrideEntity
import com.hanzi.learner.data.local.entity.StudyCountRow
import com.hanzi.learner.features.admin.model.AdminPhraseOverride
import com.hanzi.learner.features.admin.model.AdminProgress
import com.hanzi.learner.features.admin.model.AdminSettings
import com.hanzi.learner.features.admin.model.AdminStudyCount
import com.hanzi.learner.features.common.extensions.toPhraseList
import org.json.JSONArray

internal fun AppSettingsEntity.toAdminSettings(): AdminSettings = AdminSettings(
    duePickLimit = duePickLimit,
    hintAfterMisses = hintAfterMisses,
    useExternalDataset = useExternalDataset,
)

internal fun AdminSettings.toEntity(): AppSettingsEntity = AppSettingsEntity(
    id = 1,
    duePickLimit = duePickLimit,
    hintAfterMisses = hintAfterMisses,
    useExternalDataset = useExternalDataset,
)

internal fun HanziProgressEntity.toAdminProgress(): AdminProgress = AdminProgress(
    char = char,
    correctCount = correctCount,
    wrongCount = wrongCount,
    lastStudiedDay = lastStudiedDay,
    nextDueDay = nextDueDay,
    intervalDays = intervalDays,
)

internal fun StudyCountRow.toAdminStudyCount(): AdminStudyCount = AdminStudyCount(
    day = day,
    count = count,
)

internal fun PhraseOverrideEntity.toAdminPhraseOverride(): AdminPhraseOverride = AdminPhraseOverride(
    char = char,
    phrases = phrasesJson.toPhraseList(),
)

internal fun AdminPhraseOverride.toEntity(): PhraseOverrideEntity = PhraseOverrideEntity(
    char = char,
    phrasesJson = JSONArray(phrases).toString(),
)
