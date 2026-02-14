package com.hanzi.learner.feature.practice.domain

import com.hanzi.learner.db.ProgressRepositoryContract
import com.hanzi.learner.hanzi.data.CharIndexItem


class PickNextPracticeItemUseCase(
    private val progressRepository: ProgressRepositoryContract,
) : PracticeItemSelector {
    override suspend fun pickNext(request: PracticeSelectionRequest): CharIndexItem? {
        return invoke(
            index = request.index,
            disabledChars = request.disabledChars,
            excludeChars = request.excludeChars,
            limit = request.limit,
            strategy = request.strategy,
        )
    }

    suspend operator fun invoke(
        index: List<CharIndexItem>,
        disabledChars: Set<String> = emptySet(),
        excludeChars: Set<String> = emptySet(),
        limit: Int = 50,
        strategy: PracticeItemSelectionStrategy = PracticeItemSelectionStrategy.DueThenNew,
    ): CharIndexItem? {
        if (index.isEmpty()) return null

        val availableIndex = index.filter { item ->
            (disabledChars.isEmpty() || item.char !in disabledChars) &&
                (excludeChars.isEmpty() || item.char !in excludeChars)
        }
        if (availableIndex.isEmpty()) return null

        val byChar = availableIndex.associateBy { it.char }
        val dueChars = progressRepository.getDueChars(limit = limit)
        val knownChars = progressRepository.getAllLearnedChars().toHashSet()

        fun pickDue(): CharIndexItem? {
            for (ch in dueChars) {
                val item = byChar[ch]
                if (item != null) return item
            }
            return null
        }

        fun pickNew(): CharIndexItem? {
            for (item in availableIndex) {
                if (item.char !in knownChars) return item
            }
            return null
        }

        return when (strategy) {
            PracticeItemSelectionStrategy.ReviewOnly -> pickDue()
            PracticeItemSelectionStrategy.DueThenNew -> pickDue() ?: pickNew()
            PracticeItemSelectionStrategy.NewThenDue -> pickNew() ?: pickDue()
        }
    }
}
