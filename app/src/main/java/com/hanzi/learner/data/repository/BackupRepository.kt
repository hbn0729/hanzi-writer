package com.hanzi.learner.data

class BackupRepository(
    private val progressDao: HanziProgressDao,
    private val phraseOverrideDao: PhraseOverrideDao,
    private val disabledCharDao: DisabledCharDao,
    private val appSettingsDao: AppSettingsDao,
) : BackupRepositoryContract {
    override suspend fun read(options: ExportOptions): BackupData {
        val progress = if (options.progress) progressDao.getAll() else emptyList()
        val phraseOverrides = if (options.phraseOverrides) phraseOverrideDao.getAll() else emptyList()
        val disabledChars = if (options.disabledChars) disabledCharDao.getAllDisabledChars() else emptyList()
        val settings = if (options.settings) (appSettingsDao.get() ?: AppSettingsEntity()) else null

        return BackupData(
            version = 1,
            progress = progress,
            phraseOverrides = phraseOverrides,
            disabledChars = disabledChars,
            settings = settings,
        )
    }

    override suspend fun replaceAll(data: BackupData) {
        progressDao.deleteAll()
        phraseOverrideDao.deleteAll()
        disabledCharDao.deleteAll()
        writeAll(data)
    }

    override suspend fun mergeAll(data: BackupData) {
        writeAll(data)
    }

    private suspend fun writeAll(data: BackupData) {
        for (p in data.progress) progressDao.upsert(p)
        for (po in data.phraseOverrides) phraseOverrideDao.upsert(po)
        for (ch in data.disabledChars) disabledCharDao.disable(DisabledCharEntity(char = ch))
        val settings = data.settings
        if (settings != null) {
            appSettingsDao.upsert(settings.copy(id = 1))
        }
    }
}
