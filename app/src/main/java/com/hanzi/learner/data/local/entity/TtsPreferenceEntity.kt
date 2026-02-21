package com.hanzi.learner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for storing user's TTS model preference.
 * Uses single-row pattern with fixed ID = 1.
 */
@Entity(tableName = "tts_preference")
data class TtsPreferenceEntity(
    @PrimaryKey val id: Int = 1,
    val selectedModelId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)