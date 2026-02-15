package com.hanzi.learner.features.admin.repository

import com.hanzi.learner.data.local.dao.AppSettingsDao
import com.hanzi.learner.features.admin.model.AdminSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminAppSettingsRepositoryImpl(
    private val appSettingsDao: AppSettingsDao,
) : AdminAppSettingsRepository {
    override suspend fun getSettings(): AdminSettings = withContext(Dispatchers.IO) {
        (appSettingsDao.get() ?: com.hanzi.learner.data.local.entity.AppSettingsEntity()).toAdminSettings()
    }

    override suspend fun updateSettings(settings: AdminSettings) = withContext(Dispatchers.IO) {
        appSettingsDao.upsert(settings.toEntity())
    }
}
