package com.hanzi.learner.feature.admin.repository

import com.hanzi.learner.db.DisabledCharDao
import com.hanzi.learner.db.DisabledCharEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminDisabledCharRepositoryImpl(
    private val disabledCharDao: DisabledCharDao,
) : AdminDisabledCharRepository {
    override suspend fun getDisabledChars(): Set<String> = withContext(Dispatchers.IO) { disabledCharDao.getAllDisabledChars().toHashSet() }

    override suspend fun enableCharacter(char: String) = withContext(Dispatchers.IO) { disabledCharDao.enable(char) }

    override suspend fun disableCharacter(char: String) = withContext(Dispatchers.IO) { disabledCharDao.disable(DisabledCharEntity(char)) }

    override suspend fun disableAll(chars: List<String>) { withContext(Dispatchers.IO) { disabledCharDao.disableAll(chars.map { DisabledCharEntity(it) }) } }

    override suspend fun enableAll(chars: List<String>) { withContext(Dispatchers.IO) { disabledCharDao.enableAll(chars) } }

    override suspend fun deleteAllDisabledChars() { withContext(Dispatchers.IO) { disabledCharDao.deleteAll() } }
}
