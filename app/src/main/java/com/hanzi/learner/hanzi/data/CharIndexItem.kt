package com.hanzi.learner.hanzi.data

data class CharIndexItem(
    val char: String,
    val codepoint: Int,
    val file: String,
    val pinyin: List<String>,
    val strokeCount: Int,
    val phrases: List<String>,
)
