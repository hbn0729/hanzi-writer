package com.hanzi.learner.data

interface AppSettingsRepositoryContract {
    suspend fun getSettings(): AppSettings

    suspend fun updateSettings(settings: AppSettings)
}
