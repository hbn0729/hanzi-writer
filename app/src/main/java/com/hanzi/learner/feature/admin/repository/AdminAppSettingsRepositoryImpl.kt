package com.hanzi.learner.feature.admin.repository

import com.hanzi.learner.db.AppSettingsDao
import com.hanzi.learner.feature.admin.model.AdminSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminAppSettingsRepositoryImpl(
    private val appSettingsDao: AppSettingsDao,
) : AdminAppSettingsRepository {
    override suspend fun getSettings(): AdminSettings = withContext(Dispatchers.IO) {
        (appSettingsDao.get() ?: com.hanzi.learner.db.AppSettingsEntity()).toAdminSettings()
    }

    override suspend fun updateSettings(settings: AdminSettings) = withContext(Dispatchers.IO) {
        appSettingsDao.upsert(settings.toEntity())
    }
}
