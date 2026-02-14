package com.hanzi.learner.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phrase_override")
data class PhraseOverrideEntity(
    @PrimaryKey val char: String,
    val phrasesJson: String,
)
