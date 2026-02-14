package com.hanzi.learner.hanzi.model

data class CharacterData(
    val char: String,
    val strokes: List<String>,
    val medians: List<List<Point>>,
)
