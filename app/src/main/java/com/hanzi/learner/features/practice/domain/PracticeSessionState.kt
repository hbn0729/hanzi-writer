package com.hanzi.learner.features.practice.domain

import com.hanzi.learner.character_writer.data.CharIndexItem
import com.hanzi.learner.character_writer.model.CharacterData

data class PracticeSessionState(
    val isSessionComplete: Boolean = false,
    val allDisabled: Boolean = false,
    val noReviewsDue: Boolean = false,
    val currentCharacter: CharacterData? = null,
    val currentItem: CharIndexItem? = null,
    val currentPhrase: String = "",
    val strokeIndex: Int = 0,
    val completedStrokeCount: Int = 0,
    val mistakesOnStroke: Int = 0,
    val hintAfterMisses: Int = 2,
    val windowItems: List<CharIndexItem> = emptyList(),
)
