package com.hanzi.learner.character_writer.data

import kotlinx.collections.immutable.ImmutableList

data class CharIndexItem(
    val char: String,
    val codepoint: Int,
    val file: String,
    val pinyin: ImmutableList<String>,
    val strokeCount: Int,
    val phrases: ImmutableList<String>,
)
