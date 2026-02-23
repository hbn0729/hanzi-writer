package com.hanzi.learner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tts_preference")
data class TtsPreferenceEntity(
    @PrimaryKey val id: Int = 1,
    val selectedModelId: String? = null,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val updatedAt: Long = System.currentTimeMillis(),
)