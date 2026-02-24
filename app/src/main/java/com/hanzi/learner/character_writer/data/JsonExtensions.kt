package com.hanzi.learner.character_writer.data

import com.hanzi.learner.character_writer.model.Point
import org.json.JSONArray
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

fun JSONArray.toStringList(): ImmutableList<String> = buildList(length()) {
    for (i in 0 until length()) add(getString(i))
}.toImmutableList()

fun JSONArray.toMedians(): ImmutableList<ImmutableList<Point>> = buildList(length()) {
    for (strokeIndex in 0 until length()) {
        val stroke = getJSONArray(strokeIndex)
        add(stroke.toPoints())
    }
}.toImmutableList()

fun JSONArray.toPoints(): ImmutableList<Point> = buildList(length()) {
    for (i in 0 until length()) {
        val pair = getJSONArray(i)
        add(Point(x = pair.getDouble(0).toFloat(), y = pair.getDouble(1).toFloat()))
    }
}.toImmutableList()
