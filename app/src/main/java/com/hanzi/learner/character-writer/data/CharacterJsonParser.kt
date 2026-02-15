package com.hanzi.learner.character-writer.data

import com.hanzi.learner.character-writer.model.CharacterData
import org.json.JSONArray
import org.json.JSONObject

internal object CharacterJsonParser {
    fun parseIndex(json: String): List<CharIndexItem> {
        val arr = JSONArray(json)
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                add(
                    CharIndexItem(
                        char = obj.getString("char"),
                        codepoint = obj.getInt("codepoint"),
                        file = obj.getString("file"),
                        pinyin = obj.optJSONArray("pinyin")?.toStringList().orEmpty(),
                        strokeCount = obj.optInt("strokeCount", 0),
                        phrases = obj.optJSONArray("phrases")?.toStringList().orEmpty(),
                    )
                )
            }
        }
    }

    fun parseCharacter(item: CharIndexItem, json: String): CharacterData {
        val obj = JSONObject(json)
        val strokes = obj.getJSONArray("strokes").toStringList()
        val medians = obj.getJSONArray("medians").toMedians()
        return CharacterData(
            char = item.char,
            strokes = strokes,
            medians = medians,
        )
    }
}
