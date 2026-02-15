package com.hanzi.learner.features.admin.domain

import com.hanzi.learner.features.admin.model.AdminProgress
import com.hanzi.learner.features.admin.repository.AdminDisabledCharRepository
import com.hanzi.learner.features.admin.repository.AdminIndexRepository
import com.hanzi.learner.features.admin.repository.AdminProgressQueryRepository

class AdminCharacterDataLoaderImpl(
    private val indexRepository: AdminIndexRepository,
    private val disabledCharRepository: AdminDisabledCharRepository,
    private val progressQueryRepository: AdminProgressQueryRepository,
) : AdminCharacterDataLoader {
    override suspend fun load(): AdminCharacterData {
        val indexItems = indexRepository.loadIndex()
        val disabledChars = disabledCharRepository.getDisabledChars()
        val allProgress = progressQueryRepository.getAllProgress()

        return AdminCharacterData(
            indexItems = indexItems,
            disabledChars = disabledChars,
            allProgress = allProgress,
        )
    }
}
