package com.hanzi.learner.db

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
