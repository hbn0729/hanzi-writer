package com.hanzi.learner.data.repository

import com.hanzi.learner.data.local.dao.HanziProgressDao
import com.hanzi.learner.data.local.entity.HanziProgressEntity
import com.hanzi.learner.data.model.SpacedRepetitionPolicy
import com.hanzi.learner.data.model.TimeProvider

class ProgressRepository(
    private val dao: HanziProgressDao,
    private val timeProvider: TimeProvider,
    private val policy: SpacedRepetitionPolicy,
) : ProgressRepositoryContract {
    override suspend fun recordCompletion(
        char: String,
        totalMistakes: Int,
    ) {
        val today = timeProvider.todayEpochDay()
        val existing = dao.getByChar(char)
        val prevInterval = existing?.intervalDays ?: 0
        val nextInterval = policy.nextIntervalDays(previousIntervalDays = prevInterval, totalMistakes = totalMistakes)

        dao.upsert(
            HanziProgressEntity(
                char = char,
                correctCount = (existing?.correctCount ?: 0) + 1,
                wrongCount = (existing?.wrongCount ?: 0) + totalMistakes,
                lastStudiedDay = today,
                nextDueDay = today + nextInterval,
                intervalDays = nextInterval,
            )
        )
    }

    override suspend fun getDueChars(limit: Int): List<String> {
        val today = timeProvider.todayEpochDay()
        return dao.getDueChars(today = today, limit = limit)
    }

    override suspend fun getDueCount(): Int {
        val today = timeProvider.todayEpochDay()
        return dao.dueCount(today)
    }

    override suspend fun getAllLearnedChars(): List<String> {
        return dao.getAllChars()
    }
}
