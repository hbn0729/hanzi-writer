package com.hanzi.learner.feature.admin.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StrokeDatasetParserTest {
    @Test
    fun parseDictionaryLine_extractsCharacterAndPinyinArray() {
        val parser = StrokeDatasetParser()
        val rec = parser.parseDictionaryLine("""{"character":"人","pinyin":["ren2","ren4"]}""")
        assertNotNull(rec)
        assertEquals("人", rec?.char)
        assertEquals("""["ren2","ren4"]""", rec?.pinyinArrayJson)
    }

    @Test
    fun parseGraphicsLine_extractsArraysAndCountsStrokes() {
        val parser = StrokeDatasetParser()
        val line = """{"character":"人","strokes":["M0,0L1,1","M2,2L3,3"],"medians":[[[0,0],[1,1]],[[2,2],[3,3]]]}"""
        val rec = parser.parseGraphicsLine(line)
        assertNotNull(rec)
        assertEquals("人", rec?.char)
        assertEquals("""["M0,0L1,1","M2,2L3,3"]""", rec?.strokesArrayJson)
        assertEquals("""[[[0,0],[1,1]],[[2,2],[3,3]]]""", rec?.mediansArrayJson)
        assertEquals(2, rec?.strokeCount)
    }
}

