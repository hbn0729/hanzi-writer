package com.hanzi.learner.features.admin.repository

import com.hanzi.learner.data.local.dao.PhraseOverrideDao
import com.hanzi.learner.features.admin.model.AdminPhraseOverride
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminPhraseOverrideRepositoryImpl(
    private val phraseOverrideDao: PhraseOverrideDao,
) : AdminPhraseOverrideRepository {
    override suspend fun getPhraseOverrideCount(): Int = withContext(Dispatchers.IO) { phraseOverrideDao.count() }

    override suspend fun getPhraseOverride(char: String): AdminPhraseOverride? = withContext(Dispatchers.IO) {
        phraseOverrideDao.getByChar(char)?.toAdminPhraseOverride()
    }

    override suspend fun savePhraseOverride(override: AdminPhraseOverride) = withContext(Dispatchers.IO) {
        phraseOverrideDao.upsert(override.toEntity())
    }

    override suspend fun deletePhraseOverride(char: String) = withContext(Dispatchers.IO) { phraseOverrideDao.deleteByChar(char) }

    override suspend fun deleteAllPhraseOverrides() { withContext(Dispatchers.IO) { phraseOverrideDao.deleteAll() } }
}
