package com.hanzi.learner.features.admin.repository

import android.net.Uri
import com.hanzi.learner.data.model.ExportOptions
import com.hanzi.learner.data.model.ImportMode
import com.hanzi.learner.features.admin.model.AdminPhraseOverride
import com.hanzi.learner.features.admin.model.AdminProgress
import com.hanzi.learner.features.admin.model.AdminSettings
import com.hanzi.learner.features.admin.model.AdminStudyCount
import com.hanzi.learner.character-writer.data.CharIndexItem

interface AdminIndexRepository {
    suspend fun loadIndex(): List<CharIndexItem>
}

interface AdminAppSettingsRepository {
    suspend fun getSettings(): AdminSettings
    suspend fun updateSettings(settings: AdminSettings)
}

interface AdminDisabledCharRepository {
    suspend fun getDisabledChars(): Set<String>
    suspend fun enableCharacter(char: String)
    suspend fun disableCharacter(char: String)
    suspend fun disableAll(chars: List<String>)
    suspend fun enableAll(chars: List<String>)
    suspend fun deleteAllDisabledChars()
}

interface AdminProgressQueryRepository {
    suspend fun getLearnedCount(): Int
    suspend fun getDueCount(): Int
    suspend fun getTopWrong(limit: Int): List<AdminProgress>
    suspend fun getDueProgress(limit: Int): List<AdminProgress>
    suspend fun getStudyCounts(limit: Int): List<AdminStudyCount>
    suspend fun getAllProgress(): Map<String, AdminProgress>
    suspend fun getProgress(char: String): AdminProgress?
}

interface AdminProgressCommandRepository {
    suspend fun updateNextDueDay(chars: List<String>, day: Long)
    suspend fun deleteProgressByChars(chars: List<String>)
    suspend fun resetWrongCount(chars: List<String>)
    suspend fun deleteAllProgress()
}

interface AdminProgressRepository : AdminProgressQueryRepository, AdminProgressCommandRepository

interface AdminPhraseOverrideRepository {
    suspend fun getPhraseOverrideCount(): Int
    suspend fun getPhraseOverride(char: String): AdminPhraseOverride?
    suspend fun savePhraseOverride(override: AdminPhraseOverride)
    suspend fun deletePhraseOverride(char: String)
    suspend fun deleteAllPhraseOverrides()
}

data class PhraseImportResult(
    val importedChars: Int,
    val importedPhrases: Int,
)

data class CurriculumImportResult(
    val matched: Int,
    val ignored: Int,
)

data class StrokeImportResult(
    val generatedChars: Int,
    val switchedToExternalDataset: Boolean,
)

interface BackupDataTransferPort {
    suspend fun exportData(uri: Uri, options: ExportOptions)
    suspend fun importData(uri: Uri, mode: ImportMode)
}

interface PhraseImportPort {
    suspend fun importPhrases(uri: Uri): PhraseImportResult
}

interface CurriculumImportPort {
    suspend fun importCurriculum(uri: Uri, disableOthers: Boolean, indexItems: List<CharIndexItem>): CurriculumImportResult
}

interface StrokeImportPort {
    suspend fun importStrokes(uri: Uri): StrokeImportResult
}
