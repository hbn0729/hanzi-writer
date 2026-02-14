package com.hanzi.learner.feature.admin.repository

import com.hanzi.learner.db.AppSettingsEntity
import com.hanzi.learner.db.HanziProgressEntity
import com.hanzi.learner.db.PhraseOverrideEntity
import com.hanzi.learner.db.StudyCountRow
import com.hanzi.learner.feature.admin.model.AdminPhraseOverride
import com.hanzi.learner.feature.admin.model.AdminProgress
import com.hanzi.learner.feature.admin.model.AdminSettings
import com.hanzi.learner.feature.admin.model.AdminStudyCount
import com.hanzi.learner.feature.common.extensions.toPhraseList
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
