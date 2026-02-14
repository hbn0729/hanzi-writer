package com.hanzi.learner.feature.practice.domain

import com.hanzi.learner.hanzi.data.CharIndexItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PracticeSessionManagerTest {
    private fun item(char: String): CharIndexItem {
        return CharIndexItem(
            char = char,
            codepoint = char[0].code,
            file = "",
            pinyin = emptyList(),
            strokeCount = 0,
            phrases = emptyList(),
        )
    }

    @Test
    fun completesHeadAfterRequiredReps_andSlidesWindow() = runTest {
        val manager = PracticeSessionManager(windowSize = 3, requiredReps = 2)
        manager.start(listOf(item("A"), item("B"), item("C")))

        manager.recordMistake("A")
        manager.recordMistake("A")

        assertNull(manager.onCharCompleted("A") { null })
        assertEquals(1, manager.cursor)

        manager.onCharCompleted("B") { null }
        assertEquals(2, manager.cursor)

        manager.onCharCompleted("C") { null }
        assertEquals(0, manager.cursor)

        val completion = manager.onCharCompleted("A") { exclude ->
            assertEquals(manager.nextExcludes("A"), exclude)
            item("D")
        }

        assertEquals(RecordedCompletion(char = "A", totalMistakes = 2), completion)
        assertEquals(listOf("B", "C", "D"), manager.windowItems.map { it.char })
        assertEquals(0, manager.cursor)
    }

    @Test
    fun removeDisabledDropsItemsAndKeepsCursorInBounds() = runTest {
        val manager = PracticeSessionManager(windowSize = 3, requiredReps = 2)
        manager.start(listOf(item("A"), item("B"), item("C")))

        manager.onCharCompleted("B") { null }
        assertEquals(2, manager.cursor)

        val removed = manager.removeDisabled(setOf("C"))
        assertEquals(setOf("C"), removed)
        assertEquals(listOf("A", "B"), manager.windowItems.map { it.char })
        assertEquals(1, manager.cursor)
    }
}
