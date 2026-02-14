package com.hanzi.learner.feature.admin.domain

import com.hanzi.learner.feature.admin.repository.AdminDisabledCharRepository
import com.hanzi.learner.feature.admin.repository.AdminIndexRepository
import com.hanzi.learner.feature.admin.repository.AdminProgressQueryRepository

class AdminLearningDataLoaderImpl(
    private val indexRepository: AdminIndexRepository,
    private val progressQueryRepository: AdminProgressQueryRepository,
    private val disabledCharRepository: AdminDisabledCharRepository,
) : AdminLearningDataLoader {
    override suspend fun load(): AdminLearningData {
        val indexItems = indexRepository.loadIndex()
        val allProgress = progressQueryRepository.getAllProgress()
        val disabledChars = disabledCharRepository.getDisabledChars()

        return AdminLearningData(
            indexItems = indexItems,
            allProgress = allProgress,
            disabledChars = disabledChars,
        )
    }
}
