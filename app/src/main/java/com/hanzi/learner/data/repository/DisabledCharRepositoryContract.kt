package com.hanzi.learner.data.repository

interface DisabledCharRepositoryContract {
    suspend fun getAllDisabledChars(): List<String>

    suspend fun enable(char: String)

    suspend fun disable(char: String)
}
