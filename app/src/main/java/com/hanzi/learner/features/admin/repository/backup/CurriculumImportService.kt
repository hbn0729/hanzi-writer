package com.hanzi.learner.features.admin.repository.backup

import android.content.ContentResolver
import android.net.Uri
import com.hanzi.learner.data.local.dao.DisabledCharDao
import com.hanzi.learner.data.local.entity.DisabledCharEntity
import com.hanzi.learner.features.admin.repository.CurriculumImportResult
import com.hanzi.learner.character-writer.data.CharIndexItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class CurriculumImportService(
    private val contentResolver: ContentResolver,
    private val disabledCharDao: DisabledCharDao,
) {
    suspend fun import(
        uri: Uri,
        disableOthers: Boolean,
        indexItems: List<CharIndexItem>,
    ): CurriculumImportResult {
        return withContext(Dispatchers.IO) {
            val text = contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
                .trim()
            if (text.isEmpty()) throw Exception("文件为空")

            val parsedChars = parseCurriculumChars(text)
            val indexSet = indexItems.map { it.char }.toHashSet()
            val imported = parsedChars.distinct()
            val matched = imported.filter { it in indexSet }
            val missing = imported.filter { it !in indexSet }

            if (disableOthers) {
                disabledCharDao.deleteAll()
                val disabled = indexSet - matched.toHashSet()
                disabledCharDao.disableAll(disabled.map { DisabledCharEntity(it) })
            } else {
                disabledCharDao.enableAll(matched)
            }

            CurriculumImportResult(
                matched = matched.size,
                ignored = missing.size,
            )
        }
    }

    private fun parseCurriculumChars(text: String): List<String> {
        val trimmed = text.trimStart('\uFEFF').trim()
        if (trimmed.isEmpty()) return emptyList()

        if (trimmed.startsWith("[")) {
            val arr = JSONArray(trimmed)
            return buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val raw = arr.optString(i).orEmpty().trim()
                    val ch = raw.take(1)
                    if (ch.isNotEmpty()) add(ch)
                }
            }
        }

        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        val first = lines.first()
        val isCsvLike = first.contains(",") || first.contains("\t") || first.contains(";")
        val hasHeader = first.equals("char", ignoreCase = true) ||
            first.startsWith("char,", ignoreCase = true) ||
            first.startsWith("char\t", ignoreCase = true)
        val startIndex = if (hasHeader) 1 else 0

        return buildList(lines.size) {
            for (i in startIndex until lines.size) {
                val line = lines[i]
                val token = if (isCsvLike) {
                    line.split(',', '\t', ';').firstOrNull().orEmpty().trim()
                } else {
                    line
                }
                val ch = token.take(1)
                if (ch.isNotEmpty()) add(ch)
            }
        }
    }
}
