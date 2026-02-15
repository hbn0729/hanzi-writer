package com.hanzi.learner.data

interface BackupRepositoryContract {
    suspend fun read(options: ExportOptions): BackupData
    suspend fun replaceAll(data: BackupData)
    suspend fun mergeAll(data: BackupData)
}