package com.hanzi.learner.features.admin.domain

import com.hanzi.learner.data.model.TimeProvider
import com.hanzi.learner.features.admin.repository.AdminAppSettingsRepository
import com.hanzi.learner.features.admin.repository.AdminDisabledCharRepository
import com.hanzi.learner.features.admin.repository.AdminIndexRepository
import com.hanzi.learner.features.admin.repository.AdminPhraseOverrideRepository
import com.hanzi.learner.features.admin.repository.AdminProgressQueryRepository

class AdminDashboardDataLoaderImpl(
    private val indexRepository: AdminIndexRepository,
    private val appSettingsRepository: AdminAppSettingsRepository,
    private val disabledCharRepository: AdminDisabledCharRepository,
    private val progressQueryRepository: AdminProgressQueryRepository,
    private val phraseOverrideRepository: AdminPhraseOverrideRepository,
    private val timeProvider: TimeProvider,
) : AdminDashboardDataLoader {
    override suspend fun load(): AdminDashboardSnapshot {
        val indexItems = indexRepository.loadIndex()
        val disabledChars = disabledCharRepository.getDisabledChars()
        val settings = appSettingsRepository.getSettings()
        val learnedCount = progressQueryRepository.getLearnedCount()
        val dueCount = progressQueryRepository.getDueCount()
        val phraseOverrideCount = phraseOverrideRepository.getPhraseOverrideCount()
        val topWrong = progressQueryRepository.getTopWrong(limit = 20)
        val dueProgress = progressQueryRepository.getDueProgress(settings.duePickLimit)
        val studyCounts = progressQueryRepository.getStudyCounts(limit = 30)

        val totalChars = indexItems.size
        val disabledCount = disabledChars.size
        val enabledCount = (totalChars - disabledCount).coerceAtLeast(0)
        val unlearnedCount = (totalChars - learnedCount).coerceAtLeast(0)

        return AdminDashboardSnapshot(
            indexItems = indexItems,
            disabledChars = disabledChars,
            totalChars = totalChars,
            enabledCount = enabledCount,
            disabledCount = disabledCount,
            learnedCount = learnedCount,
            unlearnedCount = unlearnedCount,
            dueCount = dueCount,
            phraseOverrideCount = phraseOverrideCount,
            topWrong = topWrong,
            dueProgress = dueProgress,
            studyCounts = studyCounts,
        )
    }
}
