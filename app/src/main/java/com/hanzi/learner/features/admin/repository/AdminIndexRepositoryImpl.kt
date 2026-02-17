package com.hanzi.learner.features.admin.repository

import com.hanzi.learner.data.local.dao.AppSettingsDao
import com.hanzi.learner.data.local.entity.AppSettingsEntity
import com.hanzi.learner.character_writer.data.CharIndexItem
import com.hanzi.learner.features.common.ports.CharacterRepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminIndexRepositoryImpl(
    private val characterRepositoryProvider: CharacterRepositoryProvider,
    private val appSettingsDao: AppSettingsDao,
) : AdminIndexRepository {
    override suspend fun loadIndex(): List<CharIndexItem> {
        val settings = appSettingsDao.get() ?: AppSettingsEntity()
        val repo = characterRepositoryProvider.get(settings.useExternalDataset)
        return withContext(Dispatchers.IO) { repo.loadIndex() }
    }
}

