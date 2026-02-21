package com.hanzi.learner.speech.internal

import org.junit.Assert.*
import org.junit.Test

class SherpaOnnxTtsEngineTest {

    @Test
    fun `TtsModelConfig has useFilesystem field with default value false`() {
        val config = SherpaOnnxTtsEngine.TtsModelConfig(
            modelPath = "test/model.onnx"
        )

        assertFalse("useFilesystem should default to false", config.useFilesystem)
    }

    @Test
    fun `TtsModelConfig can set useFilesystem to true`() {
        val config = SherpaOnnxTtsEngine.TtsModelConfig(
            modelPath = "test/model.onnx",
            useFilesystem = true
        )

        assertTrue("useFilesystem should be true", config.useFilesystem)
    }

    @Test
    fun `TtsModelConfig preserves all fields`() {
        val config = SherpaOnnxTtsEngine.TtsModelConfig(
            modelPath = "/absolute/path/model.onnx",
            lexiconPath = "/absolute/path/lexicon.txt",
            tokensPath = "/absolute/path/tokens.txt",
            dataDir = "/absolute/path/data",
            dictDir = "/absolute/path/dict",
            useFilesystem = true
        )

        assertEquals("modelPath should be preserved", "/absolute/path/model.onnx", config.modelPath)
        assertEquals("lexiconPath should be preserved", "/absolute/path/lexicon.txt", config.lexiconPath)
        assertEquals("tokensPath should be preserved", "/absolute/path/tokens.txt", config.tokensPath)
        assertEquals("dataDir should be preserved", "/absolute/path/data", config.dataDir)
        assertEquals("dictDir should be preserved", "/absolute/path/dict", config.dictDir)
        assertTrue("useFilesystem should be true", config.useFilesystem)
    }

    @Test
    fun `TtsModelConfig with useFilesystem false uses asset mode`() {
        val config = SherpaOnnxTtsEngine.TtsModelConfig(
            modelPath = "tts_models/model.onnx",
            useFilesystem = false
        )

        assertFalse("useFilesystem should be false for asset mode", config.useFilesystem)
    }
}