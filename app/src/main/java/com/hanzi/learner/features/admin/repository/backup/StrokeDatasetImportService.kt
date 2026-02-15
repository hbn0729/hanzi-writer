package com.hanzi.learner.features.admin.repository.backup

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.hanzi.learner.data.local.dao.AppSettingsDao
import com.hanzi.learner.data.local.entity.AppSettingsEntity
import com.hanzi.learner.features.admin.backup.BackupZipExtractor
import com.hanzi.learner.features.admin.backup.ExtractedMakemeahanziFiles
import com.hanzi.learner.features.admin.backup.StrokeDatasetParser
import com.hanzi.learner.features.admin.backup.StrokeDatasetWriter
import com.hanzi.learner.features.admin.backup.StrokeGraphicsRecord
import com.hanzi.learner.features.admin.repository.StrokeImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "StrokeDatasetImport"

class StrokeDatasetImportService(
    private val contentResolver: ContentResolver,
    private val filesDir: File,
    private val cacheDir: File,
    private val appSettingsDao: AppSettingsDao,
    private val backupZipExtractor: BackupZipExtractor,
    private val strokeDatasetParser: StrokeDatasetParser,
    private val strokeDatasetWriter: StrokeDatasetWriter,
) {
    suspend fun import(uri: Uri): StrokeImportResult {
        return withContext(Dispatchers.IO) {
            val datasetDir = prepareDatasetDirectory()
            val graphicsTemp = File(cacheDir, "makemeahanzi_graphics.txt")
            val dictTemp = File(cacheDir, "makemeahanzi_dictionary.txt")

            val extracted = extractZipFiles(uri, graphicsTemp, dictTemp)
            val pinyinMap = parseDictionary(extracted.dictionaryFile)
            val graphicsRecords = parseGraphics(extracted.graphicsFile)
            val count = writeDatasetAndUpdateSettings(datasetDir, graphicsRecords, pinyinMap)

            StrokeImportResult(
                generatedChars = count,
                switchedToExternalDataset = true,
            )
        }
    }

    private fun prepareDatasetDirectory(): File {
        val datasetDir = File(filesDir, "hanzi_dataset")
        datasetDir.deleteRecursively()
        datasetDir.mkdirs()
        return datasetDir
    }

    private fun extractZipFiles(uri: Uri, graphicsTemp: File, dictTemp: File): ExtractedMakemeahanziFiles {
        return backupZipExtractor.extractMakemeahanziFiles(
            uri = uri,
            graphicsTemp = graphicsTemp,
            dictionaryTemp = dictTemp,
        )
    }

    private fun parseDictionary(dictionaryFile: File?): Map<String, String> {
        val pinyinMap = HashMap<String, String>(4096)
        dictionaryFile?.bufferedReader(Charsets.UTF_8)?.useLines { lines ->
            var dictParseErrors = 0
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    try {
                        val record = strokeDatasetParser.parseDictionaryLine(trimmed)
                        if (record != null) pinyinMap[record.char] = record.pinyinArrayJson
                    } catch (e: Exception) {
                        dictParseErrors++
                        if (dictParseErrors <= 5) {
                            Log.w(TAG, "dictionary.txt 行解析失败: ${e.message}", e)
                        }
                    }
                }
            }
            if (dictParseErrors > 0) {
                Log.w(TAG, "dictionary.txt 解析失败行数: $dictParseErrors")
            }
        }
        return pinyinMap
    }

    private fun parseGraphics(graphicsFile: File): List<StrokeGraphicsRecord> {
        val graphicsRecords = ArrayList<StrokeGraphicsRecord>(8192)
        var graphicsParseErrors = 0
        graphicsFile.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    try {
                        val record = strokeDatasetParser.parseGraphicsLine(trimmed)
                        if (record != null) graphicsRecords.add(record)
                    } catch (e: Exception) {
                        graphicsParseErrors++
                        if (graphicsParseErrors <= 5) {
                            Log.w(TAG, "graphics.txt 行解析失败: ${e.message}", e)
                        }
                    }
                }
            }
        }
        if (graphicsParseErrors > 0) {
            Log.w(TAG, "graphics.txt 解析失败行数: $graphicsParseErrors")
        }
        return graphicsRecords
    }

    private suspend fun writeDatasetAndUpdateSettings(
        datasetDir: File,
        graphicsRecords: List<StrokeGraphicsRecord>,
        pinyinMap: Map<String, String>,
    ): Int {
        val count = strokeDatasetWriter.writeDataset(
            datasetDir = datasetDir,
            graphicsRecords = graphicsRecords,
            pinyinByChar = pinyinMap,
        )

        val current = appSettingsDao.get() ?: AppSettingsEntity()
        appSettingsDao.upsert(current.copy(useExternalDataset = true))

        return count
    }
}
