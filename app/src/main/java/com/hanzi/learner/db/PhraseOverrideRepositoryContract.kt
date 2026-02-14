package com.hanzi.learner.db

data class PhraseOverrideData(
    val char: String,
    val phrases: List<String>,
)

interface PhraseOverrideRepositoryContract {
    suspend fun getByChar(char: String): PhraseOverrideData?
}
