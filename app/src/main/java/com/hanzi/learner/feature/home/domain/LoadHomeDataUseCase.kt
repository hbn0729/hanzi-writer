package com.hanzi.learner.feature.home.domain

import com.hanzi.learner.db.DisabledCharRepositoryContract
import com.hanzi.learner.db.ProgressRepositoryContract

data class HomeCounts(
    val unlearnedCount: Int,
    val reviewCount: Int,
)

class LoadHomeDataUseCase(
    private val progressRepository: ProgressRepositoryContract,
    private val disabledCharRepository: DisabledCharRepositoryContract,
    private val resolveCharacterRepositoryUseCase: ResolveHomeCharacterRepositoryUseCase,
) {
    suspend operator fun invoke(): HomeCounts {
        val characterRepository = resolveCharacterRepositoryUseCase()
        val index = characterRepository.loadIndex()
        val disabled = disabledCharRepository.getAllDisabledChars().toHashSet()
        val learned = progressRepository.getAllLearnedChars().toHashSet()
        val unlearnedCount = index.count { it.char !in disabled && it.char !in learned }
        val reviewCount = progressRepository.getDueCount()
        return HomeCounts(unlearnedCount = unlearnedCount, reviewCount = reviewCount)
    }
}
