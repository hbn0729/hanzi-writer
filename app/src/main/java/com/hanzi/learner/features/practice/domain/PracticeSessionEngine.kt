package com.hanzi.learner.features.practice.domain

data class RecordedCompletion(
    val char: String,
    val totalMistakes: Int,
)

data class PracticeCompletionResult(
    val completion: RecordedCompletion?,
    val state: PracticeSessionState,
)

interface PracticeSessionEngine {
    fun recordMistake(char: String)
    suspend fun startSession(): PracticeSessionState
    suspend fun processCharCompletion(finishedChar: String): PracticeCompletionResult
}

interface PracticeSessionEngineFactory {
    fun create(reviewOnly: Boolean): PracticeSessionEngine
}
