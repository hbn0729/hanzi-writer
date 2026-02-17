package com.hanzi.learner.character_writer.model

data class CharacterData(
    val char: String,
    val strokes: List<String>,
    val medians: List<List<Point>>,
)
