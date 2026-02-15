package com.hanzi.learner.features.practice.domain

import com.hanzi.learner.character-writer.data.CharIndexItem

enum class PracticeItemSelectionStrategy {
    ReviewOnly,
    DueThenNew,
    NewThenDue,
}

data class PracticeSelectionRequest(
    val index: List<CharIndexItem>,
    val disabledChars: Set<String> = emptySet(),
    val excludeChars: Set<String> = emptySet(),
    val limit: Int = 50,
    val strategy: PracticeItemSelectionStrategy = PracticeItemSelectionStrategy.DueThenNew,
)

interface PracticeItemSelector {
    suspend fun pickNext(request: PracticeSelectionRequest): CharIndexItem?
}
