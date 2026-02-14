package com.hanzi.learner.feature.admin.backup

data class StrokeDictionaryRecord(
    val char: String,
    val pinyinArrayJson: String,
)

data class StrokeGraphicsRecord(
    val char: String,
    val strokesArrayJson: String,
    val mediansArrayJson: String,
    val strokeCount: Int,
)

class StrokeDatasetParser {
    fun parseDictionaryLine(line: String): StrokeDictionaryRecord? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        val ch = extractJsonStringField(trimmed, "character")?.take(1).orEmpty()
        if (ch.isEmpty()) return null

        val pinyin = extractJsonArray(trimmed, "pinyin") ?: "[]"
        return StrokeDictionaryRecord(char = ch, pinyinArrayJson = pinyin)
    }

    fun parseGraphicsLine(line: String): StrokeGraphicsRecord? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        val ch = extractJsonStringField(trimmed, "character")?.take(1).orEmpty()
        if (ch.isEmpty()) return null

        val strokes = extractJsonArray(trimmed, "strokes") ?: return null
        val medians = extractJsonArray(trimmed, "medians") ?: return null
        val strokeCount = countTopLevelStringElementsInJsonArray(strokes)
        if (strokeCount <= 0) return null

        return StrokeGraphicsRecord(
            char = ch,
            strokesArrayJson = strokes,
            mediansArrayJson = medians,
            strokeCount = strokeCount,
        )
    }

    private fun extractJsonStringField(json: String, key: String): String? {
        val needle = "\"$key\""
        val keyIndex = json.indexOf(needle)
        if (keyIndex < 0) return null
        val colonIndex = json.indexOf(':', startIndex = keyIndex + needle.length)
        if (colonIndex < 0) return null

        var i = colonIndex + 1
        while (i < json.length && json[i].isWhitespace()) i++
        if (i >= json.length || json[i] != '"') return null
        i++

        val sb = StringBuilder()
        var escaped = false
        while (i < json.length) {
            val c = json[i]
            if (escaped) {
                sb.append(c)
                escaped = false
            } else {
                when (c) {
                    '\\' -> escaped = true
                    '"' -> return sb.toString()
                    else -> sb.append(c)
                }
            }
            i++
        }
        return null
    }

    private fun extractJsonArray(json: String, key: String): String? {
        val needle = "\"$key\""
        val keyIndex = json.indexOf(needle)
        if (keyIndex < 0) return null
        val colonIndex = json.indexOf(':', startIndex = keyIndex + needle.length)
        if (colonIndex < 0) return null

        var i = colonIndex + 1
        while (i < json.length && json[i].isWhitespace()) i++
        if (i >= json.length || json[i] != '[') return null

        val start = i
        var depth = 0
        var inString = false
        var escaped = false
        while (i < json.length) {
            val c = json[i]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else {
                    when (c) {
                        '\\' -> escaped = true
                        '"' -> inString = false
                    }
                }
            } else {
                when (c) {
                    '"' -> inString = true
                    '[' -> depth++
                    ']' -> {
                        depth--
                        if (depth == 0) return json.substring(start, i + 1)
                    }
                }
            }
            i++
        }
        return null
    }

    private fun countTopLevelStringElementsInJsonArray(arrayJson: String): Int {
        val s = arrayJson.trim()
        if (!s.startsWith('[') || !s.endsWith(']')) return 0

        var i = 0
        var depth = 0
        var inString = false
        var escaped = false
        var count = 0
        var sawStringStartAtDepth1 = false

        while (i < s.length) {
            val c = s[i]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else {
                    when (c) {
                        '\\' -> escaped = true
                        '"' -> {
                            inString = false
                            if (depth == 1 && sawStringStartAtDepth1) {
                                count++
                                sawStringStartAtDepth1 = false
                            }
                        }
                    }
                }
            } else {
                when (c) {
                    '[' -> depth++
                    ']' -> depth--
                    '"' -> {
                        inString = true
                        if (depth == 1) {
                            sawStringStartAtDepth1 = true
                        }
                    }
                }
            }
            i++
        }
        return count
    }
}

