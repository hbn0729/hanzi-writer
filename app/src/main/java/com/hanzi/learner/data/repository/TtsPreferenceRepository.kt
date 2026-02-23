package com.hanzi.learner.data.repository

import com.hanzi.learner.data.local.dao.TtsPreferenceDao
import com.hanzi.learner.data.local.entity.TtsPreferenceEntity
import com.hanzi.learner.speech.model.TtsSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TtsPreferenceRepository(
    private val dao: TtsPreferenceDao,
) : TtsPreferenceRepositoryContract {

    override fun getSelectedModelId(): Flow<String?> {
        return dao.getPreferenceFlow().map { it?.selectedModelId }
    }

    override suspend fun setSelectedModelId(modelId: String?) {
        val current = dao.getPreference()
        val entity = TtsPreferenceEntity(
            selectedModelId = modelId,
            speechRate = current?.speechRate ?: TtsSettings.DEFAULT.speechRate,
            pitch = current?.pitch ?: TtsSettings.DEFAULT.pitch,
            updatedAt = System.currentTimeMillis(),
        )
        dao.upsert(entity)
    }

    override suspend fun clearSelection() {
        dao.clear()
    }

    override fun getSettings(): Flow<TtsSettings> {
        return dao.getPreferenceFlow().map { entity ->
            TtsSettings(
                speechRate = entity?.speechRate ?: TtsSettings.DEFAULT.speechRate,
                pitch = entity?.pitch ?: TtsSettings.DEFAULT.pitch,
            )
        }
    }

    override suspend fun setSpeechRate(rate: Float) {
        val current = dao.getPreference()
        val entity = TtsPreferenceEntity(
            selectedModelId = current?.selectedModelId,
            speechRate = rate.coerceIn(TtsSettings.MIN_SPEECH_RATE, TtsSettings.MAX_SPEECH_RATE),
            pitch = current?.pitch ?: TtsSettings.DEFAULT.pitch,
            updatedAt = System.currentTimeMillis(),
        )
        dao.upsert(entity)
    }

    override suspend fun setPitch(pitch: Float) {
        val current = dao.getPreference()
        val entity = TtsPreferenceEntity(
            selectedModelId = current?.selectedModelId,
            speechRate = current?.speechRate ?: TtsSettings.DEFAULT.speechRate,
            pitch = pitch.coerceIn(TtsSettings.MIN_PITCH, TtsSettings.MAX_PITCH),
            updatedAt = System.currentTimeMillis(),
        )
        dao.upsert(entity)
    }

    override suspend fun updateSettings(settings: TtsSettings) {
        val current = dao.getPreference()
        val entity = TtsPreferenceEntity(
            selectedModelId = current?.selectedModelId,
            speechRate = settings.speechRate.coerceIn(TtsSettings.MIN_SPEECH_RATE, TtsSettings.MAX_SPEECH_RATE),
            pitch = settings.pitch.coerceIn(TtsSettings.MIN_PITCH, TtsSettings.MAX_PITCH),
            updatedAt = System.currentTimeMillis(),
        )
        dao.upsert(entity)
    }
}