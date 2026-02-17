package com.hanzi.learner.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hanzi.learner.data.local.entity.AppSettingsEntity

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun get(): AppSettingsEntity?

    @Upsert
    suspend fun upsert(entity: AppSettingsEntity)
}

