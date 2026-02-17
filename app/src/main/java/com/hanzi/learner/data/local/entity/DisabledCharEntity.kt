package com.hanzi.learner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "disabled_char")
data class DisabledCharEntity(
    @PrimaryKey val char: String,
)
