package com.hanzi.learner.data

enum class ImportMode {
    Replace,
    Merge,
}

data class ExportOptions(
    val progress: Boolean = true,
    val phraseOverrides: Boolean = true,
    val disabledChars: Boolean = true,
    val settings: Boolean = true,
)
