package com.hanzi.learner.feature.practice.domain

import com.hanzi.learner.db.ProgressRepositoryContract

class CompletePracticeCharacterUseCase(
    private val progressRepository: ProgressRepositoryContract,
) {
    suspend operator fun invoke(
        engine: PracticeSessionEngine,
        finishedChar: String,
    ): PracticeSessionState {
        val result = engine.processCharCompletion(finishedChar)
        val completion = result.completion
        if (completion != null) {
            progressRepository.recordCompletion(completion.char, completion.totalMistakes)
        }
        return result.state
    }
}
