package com.hanzi.learner.data.repository

import com.hanzi.learner.data.model.AppSettings

interface AppSettingsRepositoryContract {
    suspend fun getSettings(): AppSettings

    suspend fun updateSettings(settings: AppSettings)
}
