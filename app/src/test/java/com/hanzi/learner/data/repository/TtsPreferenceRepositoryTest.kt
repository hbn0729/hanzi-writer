package com.hanzi.learner.data.repository

import com.hanzi.learner.data.local.dao.TtsPreferenceDao
import com.hanzi.learner.data.local.entity.TtsPreferenceEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TtsPreferenceRepositoryTest {

    @Test
    fun `getSelectedModelId returns null when no preference exists`() = runTest {
        val dao = object : TtsPreferenceDao {
            override fun getPreferenceFlow() = flowOf<TtsPreferenceEntity?>(null)
            override suspend fun getPreference(): TtsPreferenceEntity? = null
            override suspend fun upsert(entity: TtsPreferenceEntity) {}
            override suspend fun clear() {}
        }
        val repository = TtsPreferenceRepository(dao)

        val result = repository.getSelectedModelId().first()

        assertNull("Should return null when no preference exists", result)
    }

    @Test
    fun `getSelectedModelId returns modelId when preference exists`() = runTest {
        val entity = TtsPreferenceEntity(selectedModelId = "vits-zh-hf-fanchen-wnj")
        val dao = object : TtsPreferenceDao {
            override fun getPreferenceFlow() = flowOf(entity)
            override suspend fun getPreference(): TtsPreferenceEntity? = entity
            override suspend fun upsert(entity: TtsPreferenceEntity) {}
            override suspend fun clear() {}
        }
        val repository = TtsPreferenceRepository(dao)

        val result = repository.getSelectedModelId().first()

        assertEquals("Should return the stored model ID", "vits-zh-hf-fanchen-wnj", result)
    }

    @Test
    fun `setSelectedModelId saves modelId to DAO`() = runTest {
        var capturedEntity: TtsPreferenceEntity? = null
        val dao = object : TtsPreferenceDao {
            override fun getPreferenceFlow() = flowOf<TtsPreferenceEntity?>(null)
            override suspend fun getPreference(): TtsPreferenceEntity? = null
            override suspend fun upsert(entity: TtsPreferenceEntity) {
                capturedEntity = entity
            }
            override suspend fun clear() {}
        }
        val repository = TtsPreferenceRepository(dao)

        repository.setSelectedModelId("vits-zh-hf-theresa")

        assertNotNull("Entity should be captured", capturedEntity)
        assertEquals("Model ID should be saved", "vits-zh-hf-theresa", capturedEntity?.selectedModelId)
        assertEquals("ID should be 1", 1, capturedEntity?.id)
    }

    @Test
    fun `setSelectedModelId with null clears selection`() = runTest {
        var capturedEntity: TtsPreferenceEntity? = null
        val dao = object : TtsPreferenceDao {
            override fun getPreferenceFlow() = flowOf<TtsPreferenceEntity?>(null)
            override suspend fun getPreference(): TtsPreferenceEntity? = null
            override suspend fun upsert(entity: TtsPreferenceEntity) {
                capturedEntity = entity
            }
            override suspend fun clear() {}
        }
        val repository = TtsPreferenceRepository(dao)

        repository.setSelectedModelId(null)

        assertNotNull("Entity should be captured", capturedEntity)
        assertNull("Model ID should be null", capturedEntity?.selectedModelId)
        assertEquals("ID should be 1", 1, capturedEntity?.id)
    }

    @Test
    fun `clearSelection calls DAO clear`() = runTest {
        var clearCalled = false
        val dao = object : TtsPreferenceDao {
            override fun getPreferenceFlow() = flowOf<TtsPreferenceEntity?>(null)
            override suspend fun getPreference(): TtsPreferenceEntity? = null
            override suspend fun upsert(entity: TtsPreferenceEntity) {}
            override suspend fun clear() {
                clearCalled = true
            }
        }
        val repository = TtsPreferenceRepository(dao)

        repository.clearSelection()

        assertTrue("Clear should be called", clearCalled)
    }

    @Test
    fun `setSelectedModelId updates timestamp`() = runTest {
        val beforeTime = System.currentTimeMillis()
        var capturedEntity: TtsPreferenceEntity? = null
        val dao = object : TtsPreferenceDao {
            override fun getPreferenceFlow() = flowOf<TtsPreferenceEntity?>(null)
            override suspend fun getPreference(): TtsPreferenceEntity? = null
            override suspend fun upsert(entity: TtsPreferenceEntity) {
                capturedEntity = entity
            }
            override suspend fun clear() {}
        }
        val repository = TtsPreferenceRepository(dao)

        repository.setSelectedModelId("some-model")

        assertNotNull("Entity should be captured", capturedEntity)
        assertTrue("Timestamp should be updated", capturedEntity?.updatedAt!! >= beforeTime)
    }
}