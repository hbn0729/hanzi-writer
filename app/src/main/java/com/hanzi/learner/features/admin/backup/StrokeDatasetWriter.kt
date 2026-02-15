package com.hanzi.learner.features.admin.backup

import java.io.File

data class StrokeIndexEntry(
    val char: String,
    val codepoint: Int,
    val file: String,
    val pinyinArrayJson: String,
    val strokeCount: Int,
)

class StrokeDatasetWriter {
    fun writeDataset(
        datasetDir: File,
        graphicsRecords: List<StrokeGraphicsRecord>,
        pinyinByChar: Map<String, String>,
    ): Int {
        val charDataDir = File(datasetDir, "char_data")
        charDataDir.mkdirs()

        val indexEntries = ArrayList<StrokeIndexEntry>(graphicsRecords.size)
        for (g in graphicsRecords) {
            val codepoint = g.char[0].code
            val filename = "u${codepoint.toString(16).padStart(4, '0')}.json"
            val relative = "char_data/$filename"
            val outFile = File(charDataDir, filename)
            outFile.writeText(buildCharacterJson(g.strokesArrayJson, g.mediansArrayJson), Charsets.UTF_8)

            indexEntries.add(
                StrokeIndexEntry(
                    char = g.char,
                    codepoint = codepoint,
                    file = relative,
                    pinyinArrayJson = pinyinByChar[g.char] ?: "[]",
                    strokeCount = g.strokeCount,
                )
            )
        }

        indexEntries.sortBy { it.codepoint }
        File(datasetDir, "char_index.json").writeText(buildIndexJson(indexEntries), Charsets.UTF_8)
        return indexEntries.size
    }

    private fun buildCharacterJson(strokesArrayJson: String, mediansArrayJson: String): String {
        return "{\"strokes\":$strokesArrayJson,\"medians\":$mediansArrayJson}"
    }

    private fun buildIndexJson(entries: List<StrokeIndexEntry>): String {
        val sb = StringBuilder(entries.size * 128)
        sb.append('[')
        entries.forEachIndexed { i, e ->
            if (i > 0) sb.append(',')
            sb.append('{')
            sb.append("\"char\":\"").append(escapeJsonString(e.char)).append("\",")
            sb.append("\"codepoint\":").append(e.codepoint).append(',')
            sb.append("\"file\":\"").append(escapeJsonString(e.file)).append("\",")
            sb.append("\"pinyin\":").append(e.pinyinArrayJson).append(',')
            sb.append("\"strokeCount\":").append(e.strokeCount).append(',')
            sb.append("\"phrases\":[]")
            sb.append('}')
        }
        sb.append(']')
        return sb.toString()
    }

    private fun escapeJsonString(s: String): String {
        val sb = StringBuilder(s.length + 8)
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}

