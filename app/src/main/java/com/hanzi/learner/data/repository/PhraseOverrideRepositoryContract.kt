package com.hanzi.learner.data.repository

import com.hanzi.learner.data.model.PhraseOverrideData

interface PhraseOverrideRepositoryContract {
    suspend fun getByChar(char: String): PhraseOverrideData?
}
