package com.hanzi.learner.feature.admin.repository

import com.hanzi.learner.db.AppSettingsDao
import com.hanzi.learner.db.AppSettingsEntity
import com.hanzi.learner.hanzi.data.CharIndexItem
import com.hanzi.learner.feature.common.ports.CharacterRepositoryProvider
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

