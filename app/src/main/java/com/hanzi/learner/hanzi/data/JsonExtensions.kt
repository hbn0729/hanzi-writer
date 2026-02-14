package com.hanzi.learner.hanzi.data

import com.hanzi.learner.hanzi.model.Point
import org.json.JSONArray

fun JSONArray.toStringList(): List<String> = buildList(length()) {
    for (i in 0 until length()) add(getString(i))
}

fun JSONArray.toMedians(): List<List<Point>> = buildList(length()) {
    for (strokeIndex in 0 until length()) {
        val stroke = getJSONArray(strokeIndex)
        add(stroke.toPoints())
    }
}

fun JSONArray.toPoints(): List<Point> = buildList(length()) {
    for (i in 0 until length()) {
        val pair = getJSONArray(i)
        add(Point(x = pair.getDouble(0).toFloat(), y = pair.getDouble(1).toFloat()))
    }
}
