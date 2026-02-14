package com.hanzi.learner.feature.admin.repository

import com.hanzi.learner.db.HanziProgressDao
import com.hanzi.learner.db.TimeProvider
import com.hanzi.learner.feature.admin.model.AdminProgress
import com.hanzi.learner.feature.admin.model.AdminStudyCount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminProgressRepositoryImpl(
    private val hanziProgressDao: HanziProgressDao,
    private val timeProvider: TimeProvider,
) : AdminProgressRepository {
    override suspend fun getLearnedCount(): Int = withContext(Dispatchers.IO) { hanziProgressDao.learnedCount() }

    override suspend fun getDueCount(): Int = withContext(Dispatchers.IO) {
        hanziProgressDao.dueCount(timeProvider.todayEpochDay())
    }

    override suspend fun getTopWrong(limit: Int): List<AdminProgress> = withContext(Dispatchers.IO) {
        hanziProgressDao.getTopWrong(limit).map { it.toAdminProgress() }
    }

    override suspend fun getDueProgress(limit: Int): List<AdminProgress> = withContext(Dispatchers.IO) {
        hanziProgressDao.getDueProgress(timeProvider.todayEpochDay(), limit).map { it.toAdminProgress() }
    }

    override suspend fun getStudyCounts(limit: Int): List<AdminStudyCount> = withContext(Dispatchers.IO) {
        hanziProgressDao.getStudyCountsByDay(limit).map { it.toAdminStudyCount() }
    }

    override suspend fun getAllProgress(): Map<String, AdminProgress> = withContext(Dispatchers.IO) {
        hanziProgressDao.getAll().map { it.toAdminProgress() }.associateBy { it.char }
    }

    override suspend fun getProgress(char: String): AdminProgress? = withContext(Dispatchers.IO) {
        hanziProgressDao.getByChar(char)?.toAdminProgress()
    }

    override suspend fun updateNextDueDay(chars: List<String>, day: Long) { withContext(Dispatchers.IO) { hanziProgressDao.updateNextDueDay(chars, day) } }

    override suspend fun deleteProgressByChars(chars: List<String>) { withContext(Dispatchers.IO) { hanziProgressDao.deleteByChars(chars) } }

    override suspend fun resetWrongCount(chars: List<String>) { withContext(Dispatchers.IO) { hanziProgressDao.resetWrongCount(chars) } }

    override suspend fun deleteAllProgress() { withContext(Dispatchers.IO) { hanziProgressDao.deleteAll() } }
}
