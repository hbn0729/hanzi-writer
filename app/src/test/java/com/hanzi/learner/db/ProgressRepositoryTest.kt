package com.hanzi.learner.db

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressRepositoryTest {
    @Test
    fun getDueChars_passesTodayAndLimitToDao() = runBlocking {
        val timeProvider = object : TimeProvider {
            override fun todayEpochDay(): Long = 1234L
        }
        val dao = object : HanziProgressDao {
            var lastToday: Long? = null
            var lastLimit: Int? = null

            override suspend fun getByChar(hanziChar: String): HanziProgressEntity? = error("not used")
            override suspend fun getDueChars(today: Long, limit: Int): List<String> {
                lastToday = today
                lastLimit = limit
                return listOf("A", "B", "C").take(limit)
            }

            override suspend fun getDueProgress(today: Long, limit: Int): List<HanziProgressEntity> = error("not used")
            override suspend fun getAllChars(): List<String> = error("not used")
            override suspend fun learnedCount(): Int = error("not used")
            override suspend fun dueCount(today: Long): Int = error("not used")
            override suspend fun getTopWrong(limit: Int): List<HanziProgressEntity> = error("not used")
            override suspend fun getStudyCountsByDay(limit: Int): List<StudyCountRow> = error("not used")
            override suspend fun getAll(): List<HanziProgressEntity> = error("not used")
            override suspend fun upsert(entity: HanziProgressEntity) = error("not used")
            override suspend fun updateNextDueDay(chars: List<String>, nextDueDay: Long): Int = error("not used")
            override suspend fun resetWrongCount(chars: List<String>): Int = error("not used")
            override suspend fun deleteByChars(chars: List<String>): Int = error("not used")
            override suspend fun deleteAll() = error("not used")
        }

        val repo = ProgressRepository(
            dao = dao,
            timeProvider = timeProvider,
            policy = DefaultSpacedRepetitionPolicy,
        )
        val due = repo.getDueChars(limit = 2)

        assertEquals(listOf("A", "B"), due)
        assertEquals(1234L, dao.lastToday)
        assertEquals(2, dao.lastLimit)
    }

    @Test
    fun getAllLearnedChars_returnsDaoAllChars() = runBlocking {
        val dao = object : HanziProgressDao {
            override suspend fun getByChar(hanziChar: String): HanziProgressEntity? = error("not used")
            override suspend fun getDueChars(today: Long, limit: Int): List<String> = error("not used")
            override suspend fun getDueProgress(today: Long, limit: Int): List<HanziProgressEntity> = error("not used")
            override suspend fun getAllChars(): List<String> = listOf("A", "B")
            override suspend fun learnedCount(): Int = error("not used")
            override suspend fun dueCount(today: Long): Int = error("not used")
            override suspend fun getTopWrong(limit: Int): List<HanziProgressEntity> = error("not used")
            override suspend fun getStudyCountsByDay(limit: Int): List<StudyCountRow> = error("not used")
            override suspend fun getAll(): List<HanziProgressEntity> = error("not used")
            override suspend fun upsert(entity: HanziProgressEntity) = error("not used")
            override suspend fun updateNextDueDay(chars: List<String>, nextDueDay: Long): Int = error("not used")
            override suspend fun resetWrongCount(chars: List<String>): Int = error("not used")
            override suspend fun deleteByChars(chars: List<String>): Int = error("not used")
            override suspend fun deleteAll() = error("not used")
        }

        val repo = ProgressRepository(
            dao = dao,
            timeProvider = SystemTimeProvider,
            policy = DefaultSpacedRepetitionPolicy,
        )
        assertEquals(listOf("A", "B"), repo.getAllLearnedChars())
    }
}
