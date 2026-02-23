package com.hanzi.learner.data.repository

import com.hanzi.learner.speech.model.TtsSettings
import kotlinx.coroutines.flow.Flow

interface TtsPreferenceRepositoryContract {
    fun getSelectedModelId(): Flow<String?>
    suspend fun setSelectedModelId(modelId: String?)
    suspend fun clearSelection()
    fun getSettings(): Flow<TtsSettings>
    suspend fun setSpeechRate(rate: Float)
    suspend fun setPitch(pitch: Float)
    suspend fun updateSettings(settings: TtsSettings)
}