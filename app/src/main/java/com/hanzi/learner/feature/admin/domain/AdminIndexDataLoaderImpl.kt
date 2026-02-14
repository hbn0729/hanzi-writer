package com.hanzi.learner.feature.admin.domain

import com.hanzi.learner.feature.admin.repository.AdminIndexRepository
import com.hanzi.learner.hanzi.data.CharIndexItem

class AdminIndexDataLoaderImpl(
    private val indexRepository: AdminIndexRepository,
) : AdminIndexDataLoader {
    override suspend fun load(): List<CharIndexItem> {
        return indexRepository.loadIndex()
    }
}
