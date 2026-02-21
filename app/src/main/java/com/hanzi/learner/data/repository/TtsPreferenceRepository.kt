package com.hanzi.learner.data.repository

import com.hanzi.learner.data.local.dao.TtsPreferenceDao
import com.hanzi.learner.data.local.entity.TtsPreferenceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed implementation of TtsPreferenceRepositoryContract.
 */
class TtsPreferenceRepository(
    private val dao: TtsPreferenceDao,
) : TtsPreferenceRepositoryContract {

    override fun getSelectedModelId(): Flow<String?> {
        return dao.getPreferenceFlow().map { it?.selectedModelId }
    }

    override suspend fun setSelectedModelId(modelId: String?) {
        val entity = TtsPreferenceEntity(
            selectedModelId = modelId,
            updatedAt = System.currentTimeMillis(),
        )
        dao.upsert(entity)
    }

    override suspend fun clearSelection() {
        dao.clear()
    }
}