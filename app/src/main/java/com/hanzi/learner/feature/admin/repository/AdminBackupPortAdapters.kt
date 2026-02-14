package com.hanzi.learner.feature.admin.repository

import android.content.ContentResolver
import android.net.Uri
import com.hanzi.learner.db.AppSettingsDao
import com.hanzi.learner.db.BackupManager
import com.hanzi.learner.db.DisabledCharDao
import com.hanzi.learner.db.ExportOptions
import com.hanzi.learner.db.ImportMode
import com.hanzi.learner.db.PhraseOverrideDao
import com.hanzi.learner.feature.admin.backup.BackupZipExtractor
import com.hanzi.learner.feature.admin.backup.StrokeDatasetParser
import com.hanzi.learner.feature.admin.backup.StrokeDatasetWriter
import com.hanzi.learner.feature.admin.repository.backup.BackupDataPort
import com.hanzi.learner.feature.admin.repository.backup.CurriculumImportService
import com.hanzi.learner.feature.admin.repository.backup.PhraseImportService
import com.hanzi.learner.feature.admin.repository.backup.StrokeDatasetImportService
import com.hanzi.learner.hanzi.data.CharIndexItem
import java.io.File

class AdminBackupDataTransferPortAdapter(
    contentResolver: ContentResolver,
    backupManager: BackupManager,
) : BackupDataTransferPort {
    private val dataPort = BackupDataPort(
        contentResolver = contentResolver,
        backupManager = backupManager,
    )

    override suspend fun exportData(uri: Uri, options: ExportOptions) {
        dataPort.exportData(uri, options)
    }

    override suspend fun importData(uri: Uri, mode: ImportMode) {
        dataPort.importData(uri, mode)
    }
}

class AdminPhraseImportPortAdapter(
    contentResolver: ContentResolver,
    phraseOverrideDao: PhraseOverrideDao,
) : PhraseImportPort {
    private val phraseImportService = PhraseImportService(
        contentResolver = contentResolver,
        phraseOverrideDao = phraseOverrideDao,
    )

    override suspend fun importPhrases(uri: Uri): PhraseImportResult {
        return phraseImportService.import(uri)
    }
}

class AdminCurriculumImportPortAdapter(
    contentResolver: ContentResolver,
    disabledCharDao: DisabledCharDao,
) : CurriculumImportPort {
    private val curriculumImportService = CurriculumImportService(
        contentResolver = contentResolver,
        disabledCharDao = disabledCharDao,
    )

    override suspend fun importCurriculum(
        uri: Uri,
        disableOthers: Boolean,
        indexItems: List<CharIndexItem>,
    ): CurriculumImportResult {
        return curriculumImportService.import(uri, disableOthers, indexItems)
    }
}

class AdminStrokeImportPortAdapter(
    contentResolver: ContentResolver,
    filesDir: File,
    cacheDir: File,
    appSettingsDao: AppSettingsDao,
    backupZipExtractor: BackupZipExtractor,
    strokeDatasetParser: StrokeDatasetParser,
    strokeDatasetWriter: StrokeDatasetWriter,
) : StrokeImportPort {
    private val strokeDatasetImportService = StrokeDatasetImportService(
        contentResolver = contentResolver,
        filesDir = filesDir,
        cacheDir = cacheDir,
        appSettingsDao = appSettingsDao,
        backupZipExtractor = backupZipExtractor,
        strokeDatasetParser = strokeDatasetParser,
        strokeDatasetWriter = strokeDatasetWriter,
    )

    override suspend fun importStrokes(uri: Uri): StrokeImportResult {
        return strokeDatasetImportService.import(uri)
    }
}
