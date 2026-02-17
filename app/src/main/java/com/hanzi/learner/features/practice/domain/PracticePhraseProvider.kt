package com.hanzi.learner.features.practice.domain

import com.hanzi.learner.data.repository.PhraseOverrideRepositoryContract
import com.hanzi.learner.character_writer.data.CharIndexItem

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
