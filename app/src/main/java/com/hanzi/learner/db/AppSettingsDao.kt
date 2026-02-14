package com.hanzi.learner.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun get(): AppSettingsEntity?

    @Upsert
    suspend fun upsert(entity: AppSettingsEntity)
}

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
