package com.hanzi.learner.data

import com.hanzi.learner.data.toPhraseList

class PhraseOverrideRepository(
    private val dao: PhraseOverrideDao,
) : PhraseOverrideRepositoryContract {
    override suspend fun getByChar(char: String): PhraseOverrideData? {
        return dao.getByChar(char)?.let {
            PhraseOverrideData(
                char = it.char,
                phrases = it.phrasesJson.toPhraseList(),
            )
        }
    }
}
