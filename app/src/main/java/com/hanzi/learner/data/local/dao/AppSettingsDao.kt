package com.hanzi.learner.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun get(): AppSettingsEntity?

    @Upsert
    suspend fun upsert(entity: AppSettingsEntity)
}

