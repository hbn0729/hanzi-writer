package com.hanzi.learner.db

import com.hanzi.learner.data.model.DefaultSpacedRepetitionPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultSpacedRepetitionPolicyTest {
    @Test
    fun noMistakes_doublesInterval_andCapsAt90() {
        assertEquals(1, DefaultSpacedRepetitionPolicy.nextIntervalDays(previousIntervalDays = 0, totalMistakes = 0))
        assertEquals(1, DefaultSpacedRepetitionPolicy.nextIntervalDays(previousIntervalDays = -1, totalMistakes = 0))
        assertEquals(2, DefaultSpacedRepetitionPolicy.nextIntervalDays(previousIntervalDays = 1, totalMistakes = 0))
        assertEquals(90, DefaultSpacedRepetitionPolicy.nextIntervalDays(previousIntervalDays = 45, totalMistakes = 0))
        assertEquals(90, DefaultSpacedRepetitionPolicy.nextIntervalDays(previousIntervalDays = 90, totalMistakes = 0))
    }

    @Test
    fun mistakes_resetIntervalTo1() {
        assertEquals(1, DefaultSpacedRepetitionPolicy.nextIntervalDays(previousIntervalDays = 1, totalMistakes = 1))
        assertEquals(1, DefaultSpacedRepetitionPolicy.nextIntervalDays(previousIntervalDays = 90, totalMistakes = 3))
    }
}

