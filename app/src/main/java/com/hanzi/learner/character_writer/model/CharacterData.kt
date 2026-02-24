package com.hanzi.learner.character_writer.model

import com.hanzi.learner.character_writer.match.Stroke
import kotlinx.collections.immutable.ImmutableList

data class CharacterData(
    val char: String,
    val strokes: ImmutableList<String>,
    val medians: ImmutableList<ImmutableList<Point>>,
) {
    internal val strokeObjects: List<Stroke> by lazy {
        medians.mapIndexed { index, pts -> Stroke(points = pts, strokeNum = index) }
    }
}
