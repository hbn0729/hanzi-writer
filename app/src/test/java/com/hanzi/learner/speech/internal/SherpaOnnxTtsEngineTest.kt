package com.hanzi.learner.speech.internal

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SherpaOnnxTtsEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

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

    @Test
    fun `initialize and synthesize work in filesystem mode`() = runTest {
        val modelDir = tempFolder.newFolder("model")
        val modelFile = File(modelDir, "model.onnx").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val tokensFile = File(modelDir, "tokens.txt").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val lexiconFile = File(modelDir, "lexicon.txt").apply { writeBytes(byteArrayOf(1, 2, 3)) }

        val engine = SherpaOnnxTtsEngine(
            assetManager = null,
            modelConfig = SherpaOnnxTtsEngine.TtsModelConfig(
                modelPath = modelFile.absolutePath,
                tokensPath = tokensFile.absolutePath,
                lexiconPath = lexiconFile.absolutePath,
                useFilesystem = true,
            ),
        )

        engine.initialize()

        assertTrue(engine.isReady.value)

        val samples = engine.synthesize("你好")
        assertNotNull(samples)
        assertTrue(samples!!.isNotEmpty())
    }
}
