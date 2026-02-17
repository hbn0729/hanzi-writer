package com.hanzi.learner.data.repository

import com.hanzi.learner.data.local.dao.AppSettingsDao
import com.hanzi.learner.data.local.entity.AppSettingsEntity
import com.hanzi.learner.data.model.AppSettings

class AppSettingsRepository(
    private val dao: AppSettingsDao,
) : AppSettingsRepositoryContract {
    override suspend fun getSettings(): AppSettings {
        return (dao.get() ?: AppSettingsEntity()).toData()
    }

    override suspend fun updateSettings(settings: AppSettings) {
        dao.upsert(settings.toEntity())
    }
}

private fun AppSettingsEntity.toData(): AppSettings = AppSettings(
    duePickLimit = duePickLimit,
    hintAfterMisses = hintAfterMisses,
    useExternalDataset = useExternalDataset,
)

private fun AppSettings.toEntity(): AppSettingsEntity = AppSettingsEntity(
    duePickLimit = duePickLimit,
    hintAfterMisses = hintAfterMisses,
    useExternalDataset = useExternalDataset,
)
