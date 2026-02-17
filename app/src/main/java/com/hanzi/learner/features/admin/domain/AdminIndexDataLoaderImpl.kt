package com.hanzi.learner.features.admin.domain

import com.hanzi.learner.features.admin.repository.AdminIndexRepository
import com.hanzi.learner.character_writer.data.CharIndexItem

class AdminIndexDataLoaderImpl(
    private val indexRepository: AdminIndexRepository,
) : AdminIndexDataLoader {
    override suspend fun load(): List<CharIndexItem> {
        return indexRepository.loadIndex()
    }
}
