package com.hanzi.learner.speech.model

import org.junit.Assert.*
import org.junit.Test

class TtsModelRegistryTest {

    @Test
    fun `getAvailableModels returns at least 3 entries including system TTS and 2 SherpaOnnx models`() {
        val models = TtsModelRegistry.getAvailableModels()

        assertTrue("Should return at least 3 models", models.size >= 3)

        val systemTtsCount = models.count { it.isSystemTts }
        assertEquals("Should have exactly 1 system TTS entry", 1, systemTtsCount)

        val sherpaOnnxCount = models.count { !it.isSystemTts }
        assertTrue("Should have at least 2 SherpaOnnx models", sherpaOnnxCount >= 2)
    }

    @Test
    fun `getModelById returns correct model for valid IDs`() {
        val systemTts = TtsModelRegistry.getModelById(TtsModelRegistry.SYSTEM_TTS_ID)
        assertNotNull("Should return system TTS model", systemTts)
        assertEquals("System TTS should have correct ID", TtsModelRegistry.SYSTEM_TTS_ID, systemTts?.id)
        assertTrue("System TTS should have isSystemTts flag", systemTts?.isSystemTts == true)

        val fanchenModel = TtsModelRegistry.getModelById("vits-zh-hf-fanchen-wnj")
        assertNotNull("Should return fanchen model", fanchenModel)
        assertEquals("Fanchen model should have correct ID", "vits-zh-hf-fanchen-wnj", fanchenModel?.id)
        assertFalse("Fanchen model should not be system TTS", fanchenModel?.isSystemTts == true)
    }

    @Test
    fun `getModelById returns null for invalid ID`() {
        val result = TtsModelRegistry.getModelById("non-existent-model")
        assertNull("Should return null for invalid ID", result)
    }

    @Test
    fun `TtsModelInfo contains all required fields`() {
        val model = TtsModelRegistry.getModelById("vits-zh-hf-fanchen-wnj")
        assertNotNull("Model should exist", model)

        model?.let {
            assertNotNull("Should have id", it.id)
            assertNotNull("Should have name", it.name)
            assertNotNull("Should have description", it.description)
            assertNotNull("Should have downloadUrl", it.downloadUrl)
            assertNotNull("Should have previewAudioUrl", it.previewAudioUrl)
            assertTrue("Should have fileSizeBytes > 0", it.fileSizeBytes > 0)
            assertTrue("Should have modelFiles list", it.modelFiles.isNotEmpty())
        }
    }

    @Test
    fun `system TTS model has null downloadUrl and empty modelFiles`() {
        val systemTts = TtsModelRegistry.getModelById(TtsModelRegistry.SYSTEM_TTS_ID)
        assertNotNull("System TTS should exist", systemTts)

        systemTts?.let {
            assertNull("System TTS should have null downloadUrl", it.downloadUrl)
            assertTrue("System TTS should have empty modelFiles", it.modelFiles.isEmpty())
            assertEquals("System TTS should have 0 fileSizeBytes", 0, it.fileSizeBytes)
        }
    }

    @Test
    fun `isSystemTts returns true only for system TTS ID`() {
        assertTrue("Should return true for system TTS ID", TtsModelRegistry.isSystemTts(TtsModelRegistry.SYSTEM_TTS_ID))
        assertFalse("Should return false for SherpaOnnx model ID", TtsModelRegistry.isSystemTts("vits-zh-hf-fanchen-wnj"))
        assertFalse("Should return false for invalid ID", TtsModelRegistry.isSystemTts("invalid-id"))
    }

    @Test
    fun `TtsModelStatus enum has all required values`() {
        val statuses = TtsModelStatus.values()

        assertTrue("Should contain NOT_DOWNLOADED", statuses.contains(TtsModelStatus.NOT_DOWNLOADED))
        assertTrue("Should contain DOWNLOADING", statuses.contains(TtsModelStatus.DOWNLOADING))
        assertTrue("Should contain DOWNLOADED", statuses.contains(TtsModelStatus.DOWNLOADED))
        assertTrue("Should contain ACTIVE", statuses.contains(TtsModelStatus.ACTIVE))
        assertEquals("Should have exactly 4 statuses", 4, statuses.size)
    }
}