package com.hanzi.learner.data.model

import java.time.LocalDate

interface TimeProvider {
    fun todayEpochDay(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
}

