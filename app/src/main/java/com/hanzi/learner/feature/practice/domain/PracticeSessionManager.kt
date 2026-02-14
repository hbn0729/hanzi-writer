package com.hanzi.learner.feature.practice.domain

import com.hanzi.learner.hanzi.data.CharIndexItem

fun interface SessionCompletionRule {
    fun shouldComplete(
        cursor: Int,
        repetitionCount: Int,
        requiredRepetitions: Int,
    ): Boolean
}

object HeadItemCompletionRule : SessionCompletionRule {
    override fun shouldComplete(
        cursor: Int,
        repetitionCount: Int,
        requiredRepetitions: Int,
    ): Boolean {
        return cursor == 0 && repetitionCount >= requiredRepetitions
    }
}

interface PracticeWindowManager {
    val windowSize: Int
    val windowItems: List<CharIndexItem>

    fun reset()
    fun start(initialItems: List<CharIndexItem>)
    fun currentItem(): CharIndexItem?
    fun removeAtCursor(): String?
    fun recordMistake(char: String)
    fun removeDisabled(disabledChars: Set<String>): Set<String>
    fun nextExcludes(finishedChar: String): Set<String>
    suspend fun onCharCompleted(
        finishedChar: String,
        pickNext: suspend (excludeChars: Set<String>) -> CharIndexItem?,
    ): RecordedCompletion?
}

class PracticeSessionManager(
    override val windowSize: Int = 3,
    private val requiredReps: Int = 2,
    private val completionRule: SessionCompletionRule = HeadItemCompletionRule,
) : PracticeWindowManager {
    override var windowItems: List<CharIndexItem> = emptyList()
        private set

    var cursor: Int = 0
        private set

    private val repCountByChar = mutableMapOf<String, Int>()
    private val mistakesByChar = mutableMapOf<String, Int>()

    override fun reset() {
        windowItems = emptyList()
        cursor = 0
        repCountByChar.clear()
        mistakesByChar.clear()
    }

    override fun start(initialItems: List<CharIndexItem>) {
        reset()
        windowItems = initialItems.take(windowSize)
    }

    override fun currentItem(): CharIndexItem? = windowItems.getOrNull(cursor)

    override fun removeAtCursor(): String? {
        val removed = windowItems.getOrNull(cursor)?.char ?: return null
        val newWindow = windowItems.toMutableList()
        newWindow.removeAt(cursor)
        windowItems = newWindow
        repCountByChar.remove(removed)
        mistakesByChar.remove(removed)
        cursor = if (windowItems.isEmpty()) 0 else cursor % windowItems.size
        return removed
    }

    override fun recordMistake(char: String) {
        mistakesByChar[char] = (mistakesByChar[char] ?: 0) + 1
    }

    override fun removeDisabled(disabledChars: Set<String>): Set<String> {
        if (disabledChars.isEmpty() || windowItems.isEmpty()) return emptySet()
        val removed = windowItems.map { it.char }.filterTo(LinkedHashSet()) { it in disabledChars }
        if (removed.isEmpty()) return emptySet()
        windowItems = windowItems.filter { it.char !in disabledChars }
        removed.forEach {
            repCountByChar.remove(it)
            mistakesByChar.remove(it)
        }
        cursor = if (windowItems.isEmpty()) 0 else cursor.coerceIn(0, windowItems.size - 1)
        return removed
    }

    override fun nextExcludes(finishedChar: String): Set<String> {
        val exclude = windowItems.map { it.char }.toMutableSet()
        exclude.add(finishedChar)
        return exclude
    }

    override suspend fun onCharCompleted(
        finishedChar: String,
        pickNext: suspend (excludeChars: Set<String>) -> CharIndexItem?,
    ): RecordedCompletion? {
        val finishedIndex = windowItems.indexOfFirst { it.char == finishedChar }
        if (finishedIndex == -1) return null

        cursor = finishedIndex
        repCountByChar[finishedChar] = (repCountByChar[finishedChar] ?: 0) + 1

        val reps = repCountByChar[finishedChar] ?: 0
        if (completionRule.shouldComplete(cursor, reps, requiredReps)) {
            val completion = RecordedCompletion(
                char = finishedChar,
                totalMistakes = mistakesByChar[finishedChar] ?: 0,
            )

            repCountByChar.remove(finishedChar)
            mistakesByChar.remove(finishedChar)

            val w = windowItems.toMutableList()
            w.removeAt(0)

            windowItems = w
            val next = pickNext(nextExcludes(finishedChar))
            if (next != null) w.add(next)

            windowItems = w
            cursor = 0
            return completion
        }

        if (windowItems.size > 1) {
            cursor = (cursor + 1) % windowItems.size
        }
        return null
    }
}
