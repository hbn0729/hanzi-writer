package com.hanzi.learner.data

import com.hanzi.learner.data.repository.BackupRepositoryContract

class BackupManager(
    private val serializer: BackupSerializerContract,
    private val repository: BackupRepositoryContract,
) {
    suspend fun exportJson(
        options: ExportOptions = ExportOptions(),
    ): String {
        val data = repository.read(options)
        return serializer.encode(data)
    }

    suspend fun importJson(
        json: String,
        mode: ImportMode = ImportMode.Replace,
    ) {
        val data = serializer.decode(json)
        when (mode) {
            ImportMode.Replace -> repository.replaceAll(data)
            ImportMode.Merge -> repository.mergeAll(data)
        }
    }
}
