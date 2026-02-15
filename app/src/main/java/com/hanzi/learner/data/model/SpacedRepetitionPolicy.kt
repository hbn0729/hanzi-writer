package com.hanzi.learner.data

import kotlin.math.min

interface SpacedRepetitionPolicy {
    fun nextIntervalDays(previousIntervalDays: Int, totalMistakes: Int): Int
}

object DefaultSpacedRepetitionPolicy : SpacedRepetitionPolicy {
    override fun nextIntervalDays(previousIntervalDays: Int, totalMistakes: Int): Int {
        return if (totalMistakes == 0) {
            if (previousIntervalDays <= 0) 1 else min(previousIntervalDays * 2, 90)
        } else {
            1
        }
    }
}

