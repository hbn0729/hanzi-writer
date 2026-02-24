package com.hanzi.learner.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hanzi_progress",
    indices = [
        Index(value = ["nextDueDay"]),
        Index(value = ["lastStudiedDay"]),
        Index(value = ["wrongCount"]),
    ]
)
data class HanziProgressEntity(
    @PrimaryKey val char: String,
    val correctCount: Int,
    val wrongCount: Int,
    val lastStudiedDay: Long,
    val nextDueDay: Long,
    val intervalDays: Int,
)
