package com.hanzi.learner.speech.model

import org.junit.Assert.*
import org.junit.Test

class TtsModelRegistryTest {

    @Test
    fun `getAvailableModels returns at least system TTS`() {
        val models = TtsModelRegistry.getAvailableModels()

        assertTrue("Should return at least 1 model", models.size >= 1)

        val systemTtsCount = models.count { it.isSystemTts }
        assertEquals("Should have exactly 1 system TTS entry", 1, systemTtsCount)
    }

    @Test
    fun `getModelById returns correct model for system TTS ID`() {
        val systemTts = TtsModelRegistry.getModelById(TtsModelRegistry.SYSTEM_TTS_ID)
        assertNotNull("Should return system TTS model", systemTts)
        assertEquals("System TTS should have correct ID", TtsModelRegistry.SYSTEM_TTS_ID, systemTts?.id)
        assertTrue("System TTS should have isSystemTts flag", systemTts?.isSystemTts == true)
    }

    @Test
    fun `getModelById returns null for invalid ID`() {
        val result = TtsModelRegistry.getModelById("non-existent-model")
        assertNull("Should return null for invalid ID", result)
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
