package com.hanzi.learner.feature.practice.domain

import com.hanzi.learner.db.PhraseOverrideRepositoryContract
import com.hanzi.learner.hanzi.data.CharIndexItem

interface PracticePhraseProvider {
    suspend fun phrasesFor(item: CharIndexItem): List<String>
}

class PhraseOverridePracticePhraseProvider(
    private val phraseOverrideRepository: PhraseOverrideRepositoryContract,
) : PracticePhraseProvider {
    override suspend fun phrasesFor(item: CharIndexItem): List<String> {
        return phraseOverrideRepository.getByChar(item.char)?.phrases.orEmpty()
    }
}
