package com.hanzi.learner.features.admin.ui

import java.time.LocalDate

internal fun epochDayToText(day: Long): String {
    return if (day <= 0L) "-" else LocalDate.ofEpochDay(day).toString()
}
