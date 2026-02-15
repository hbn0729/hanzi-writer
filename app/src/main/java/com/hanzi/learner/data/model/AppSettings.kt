package com.hanzi.learner.data

data class AppSettings(
    val duePickLimit: Int = 50,
    val hintAfterMisses: Int = 2,
    val useExternalDataset: Boolean = false,
)
