package com.hanzi.learner.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val duePickLimit: Int = 50,
    val hintAfterMisses: Int = 2,
    val useExternalDataset: Boolean = false,
)
