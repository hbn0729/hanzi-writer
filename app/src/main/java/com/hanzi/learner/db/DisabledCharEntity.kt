package com.hanzi.learner.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "disabled_char")
data class DisabledCharEntity(
    @PrimaryKey val char: String,
)
