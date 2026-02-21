package com.hanzi.learner.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hanzi.learner.data.local.entity.TtsPreferenceEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for TTS preference operations.
 */
@Dao
interface TtsPreferenceDao {
    @Query("SELECT * FROM tts_preference WHERE id = 1 LIMIT 1")
    fun getPreferenceFlow(): Flow<TtsPreferenceEntity?>

    @Query("SELECT * FROM tts_preference WHERE id = 1 LIMIT 1")
    suspend fun getPreference(): TtsPreferenceEntity?

    @Upsert
    suspend fun upsert(entity: TtsPreferenceEntity)

    @Query("DELETE FROM tts_preference WHERE id = 1")
    suspend fun clear()
}