package com.hanzi.learner.feature.admin.repository.backup

import android.content.ContentResolver
import android.net.Uri
import com.hanzi.learner.db.BackupManager
import com.hanzi.learner.db.ExportOptions
import com.hanzi.learner.db.ImportMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupDataPort(
    private val contentResolver: ContentResolver,
    private val backupManager: BackupManager,
) {
    suspend fun exportData(uri: Uri, options: ExportOptions) {
        withContext(Dispatchers.IO) {
            val json = backupManager.exportJson(options)
            contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            }
        }
    }

    suspend fun importData(uri: Uri, mode: ImportMode) {
        withContext(Dispatchers.IO) {
            val json = contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
            if (json.isNotEmpty()) {
                backupManager.importJson(json, mode)
            }
        }
    }
}
