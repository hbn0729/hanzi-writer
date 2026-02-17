package com.hanzi.learner.data.repository

import com.hanzi.learner.data.model.BackupData
import com.hanzi.learner.data.model.ExportOptions

interface BackupRepositoryContract {
    suspend fun read(options: ExportOptions): BackupData
    suspend fun replaceAll(data: BackupData)
    suspend fun mergeAll(data: BackupData)
}