package com.hanzi.learner.db

data class AppSettings(
    val duePickLimit: Int = 50,
    val hintAfterMisses: Int = 2,
    val useExternalDataset: Boolean = false,
)

interface AppSettingsRepositoryContract {
    suspend fun getSettings(): AppSettings

    suspend fun updateSettings(settings: AppSettings)
}
