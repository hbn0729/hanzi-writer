package com.hanzi.learner.feature.common.extensions

import org.json.JSONArray

fun String.toPhraseList(): List<String> {
    return try {
        val arr = JSONArray(this)
        buildList(arr.length()) {
            for (i in 0 until arr.length()) add(arr.getString(i))
        }
    } catch (_: Exception) {
        emptyList()
    }
}