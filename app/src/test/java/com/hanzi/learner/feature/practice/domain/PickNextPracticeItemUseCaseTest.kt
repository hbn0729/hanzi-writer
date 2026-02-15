package com.hanzi.learner.features.practice.domain

import com.hanzi.learner.data.ProgressRepositoryContract
import com.hanzi.learner.character-writer.data.CharIndexItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PickNextPracticeItemUseCaseTest {
    private class FakeProgressRepository(
        private val dueChars: List<String> = emptyList(),
        private val learnedChars: List<String> = emptyList(),
    ) : ProgressRepositoryContract {
        override suspend fun recordCompletion(char: String, totalMistakes: Int) = error("not used")
        override suspend fun getDueChars(limit: Int): List<String> = dueChars.take(limit)

        override suspend fun getDueCount(): Int = error("not used")
        override suspend fun getAllLearnedChars(): List<String> = learnedChars
    }

    private fun item(char: String) = CharIndexItem(
        char = char,
        codepoint = char.codePointAt(0),
        file = "$char.json",
        pinyin = emptyList(),
        strokeCount = 1,
        phrases = emptyList(),
    )

    @Test
    fun practicePrefersNewCharsWhenPreferDueFalse() = runBlocking {
        val useCase = PickNextPracticeItemUseCase(
            progressRepository = FakeProgressRepository(
                dueChars = listOf("A"),
                learnedChars = listOf("A"),
            )
        )
        val index = listOf(item("A"), item("B"))
        assertEquals(
            "B",
            useCase(index = index, strategy = PracticeItemSelectionStrategy.NewThenDue)?.char
        )
    }

    @Test
    fun practiceFallsBackToDueWhenNoNewChars() = runBlocking {
        val useCase = PickNextPracticeItemUseCase(
            progressRepository = FakeProgressRepository(
                dueChars = listOf("A"),
                learnedChars = listOf("A", "B"),
            )
        )
        val index = listOf(item("A"), item("B"))
        assertEquals(
            "A",
            useCase(index = index, strategy = PracticeItemSelectionStrategy.NewThenDue)?.char
        )
    }

    @Test
    fun reviewOnlyReturnsDueOrNull() = runBlocking {
        val index = listOf(item("A"), item("B"))

        val noDue = PickNextPracticeItemUseCase(progressRepository = FakeProgressRepository(dueChars = emptyList()))
        assertNull(noDue(index = index, strategy = PracticeItemSelectionStrategy.ReviewOnly))

        val hasDue = PickNextPracticeItemUseCase(progressRepository = FakeProgressRepository(dueChars = listOf("B")))
        assertEquals("B", hasDue(index = index, strategy = PracticeItemSelectionStrategy.ReviewOnly)?.char)
    }

    @Test
    fun filtersOutDisabledAndExcludedChars() = runBlocking {
        val useCase = PickNextPracticeItemUseCase(progressRepository = FakeProgressRepository(dueChars = listOf("A", "B")))
        val index = listOf(item("A"), item("B"), item("C"))

        assertEquals(
            "B",
            useCase(
                index = index,
                disabledChars = setOf("A"),
                excludeChars = setOf("C"),
                strategy = PracticeItemSelectionStrategy.ReviewOnly,
            )?.char
        )
    }
}
