package com.hanzi.learner.feature.admin.model

data class AdminSettings(
    val duePickLimit: Int = 50,
    val hintAfterMisses: Int = 2,
    val useExternalDataset: Boolean = false,
)

data class AdminProgress(
    val char: String,
    val correctCount: Int,
    val wrongCount: Int,
    val lastStudiedDay: Long,
    val nextDueDay: Long,
    val intervalDays: Int,
)

data class AdminStudyCount(
    val day: Long,
    val count: Int,
)

data class AdminPhraseOverride(
    val char: String,
    val phrases: List<String>,
)
