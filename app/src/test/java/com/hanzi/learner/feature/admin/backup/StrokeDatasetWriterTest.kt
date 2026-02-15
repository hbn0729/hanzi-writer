package com.hanzi.learner.features.admin.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class StrokeDatasetWriterTest {
    @Test
    fun writeDataset_writesCharFilesAndIndex() {
        val tempDir = Files.createTempDirectory("hanzi_dataset_test").toFile()
        tempDir.deleteOnExit()

        val writer = StrokeDatasetWriter()
        val graphics = listOf(
            StrokeGraphicsRecord(
                char = "人",
                strokesArrayJson = """["S1","S2"]""",
                mediansArrayJson = """[[[0,0],[1,1]],[[2,2],[3,3]]]""",
                strokeCount = 2,
            )
        )
        val pinyinMap = mapOf("人" to """["ren2"]""")

        val count = writer.writeDataset(
            datasetDir = tempDir,
            graphicsRecords = graphics,
            pinyinByChar = pinyinMap,
        )

        assertEquals(1, count)

        val charFile = tempDir.resolve("char_data").resolve("u4eba.json")
        assertTrue(charFile.isFile)
        val charJson = charFile.readText(Charsets.UTF_8)
        assertTrue(charJson.contains(""""strokes":["S1","S2"]"""))
        assertTrue(charJson.contains(""""medians":[[[0,0],[1,1]],[[2,2],[3,3]]]"""))

        val indexFile = tempDir.resolve("char_index.json")
        assertTrue(indexFile.isFile)
        val indexJson = indexFile.readText(Charsets.UTF_8)
        assertTrue(indexJson.contains(""""char":"人""""))
        assertTrue(indexJson.contains(""""pinyin":["ren2"]"""))
        assertTrue(indexJson.contains(""""strokeCount":2"""))
        assertTrue(indexJson.contains(""""phrases":[]"""))
    }
}

