package com.hanzi.learner.db

interface AppSettingsRepositoryContract {
    suspend fun getSettings(): AppSettings

    suspend fun updateSettings(settings: AppSettings)
}
