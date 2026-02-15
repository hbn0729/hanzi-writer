package com.hanzi.learner.data

interface ProgressRepositoryContract {
    suspend fun recordCompletion(char: String, totalMistakes: Int)

    suspend fun getDueChars(limit: Int): List<String>

    suspend fun getDueCount(): Int

    suspend fun getAllLearnedChars(): List<String>
}
