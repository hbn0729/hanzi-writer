package com.hanzi.learner.db

interface PhraseOverrideRepositoryContract {
    suspend fun getByChar(char: String): PhraseOverrideData?
}
