package com.hanzi.learner.features.admin.domain

import com.hanzi.learner.features.admin.model.AdminProgress
import com.hanzi.learner.features.admin.model.AdminStudyCount
import com.hanzi.learner.features.admin.repository.AdminAppSettingsRepository
import com.hanzi.learner.features.admin.repository.AdminDisabledCharRepository
import com.hanzi.learner.features.admin.repository.AdminIndexRepository
import com.hanzi.learner.features.admin.repository.AdminPhraseOverrideRepository
import com.hanzi.learner.features.admin.repository.AdminProgressQueryRepository
import com.hanzi.learner.character-writer.data.CharIndexItem

data class AdminDashboardSnapshot(
    val indexItems: List<CharIndexItem>,
    val disabledChars: Set<String>,
    val totalChars: Int,
    val enabledCount: Int,
    val disabledCount: Int,
    val learnedCount: Int,
    val unlearnedCount: Int,
    val dueCount: Int,
    val phraseOverrideCount: Int,
    val topWrong: List<AdminProgress>,
    val dueProgress: List<AdminProgress>,
    val studyCounts: List<AdminStudyCount>,
)

class LoadAdminDashboardUseCase(
    private val indexRepository: AdminIndexRepository,
    private val appSettingsRepository: AdminAppSettingsRepository,
    private val disabledCharRepository: AdminDisabledCharRepository,
    private val progressRepository: AdminProgressQueryRepository,
    private val phraseOverrideRepository: AdminPhraseOverrideRepository,
) {
    suspend operator fun invoke(): AdminDashboardSnapshot {
        val indexItems = indexRepository.loadIndex()
        val disabledChars = disabledCharRepository.getDisabledChars()
        val settings = appSettingsRepository.getSettings()
        val learnedCount = progressRepository.getLearnedCount()
        val dueCount = progressRepository.getDueCount()
        val phraseOverrideCount = phraseOverrideRepository.getPhraseOverrideCount()
        val topWrong = progressRepository.getTopWrong(20)
        val dueProgress = progressRepository.getDueProgress(settings.duePickLimit)
        val studyCounts = progressRepository.getStudyCounts(30)

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
