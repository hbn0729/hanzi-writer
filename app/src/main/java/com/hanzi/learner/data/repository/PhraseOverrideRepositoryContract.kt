package com.hanzi.learner.data

interface PhraseOverrideRepositoryContract {
    suspend fun getByChar(char: String): PhraseOverrideData?
}
