package com.hanzi.learner.character_writer.data

data class CharIndexItem(
    val char: String,
    val codepoint: Int,
    val file: String,
    val pinyin: List<String>,
    val strokeCount: Int,
    val phrases: List<String>,
)
