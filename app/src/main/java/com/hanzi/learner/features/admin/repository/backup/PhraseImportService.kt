package com.hanzi.learner.features.admin.repository.backup

import android.content.ContentResolver
import android.net.Uri
import com.hanzi.learner.data.local.dao.PhraseOverrideDao
import com.hanzi.learner.data.local.entity.PhraseOverrideEntity
import com.hanzi.learner.features.admin.repository.PhraseImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class PhraseImportService(
    private val contentResolver: ContentResolver,
    private val phraseOverrideDao: PhraseOverrideDao,
) {
    suspend fun import(uri: Uri): PhraseImportResult {
        return withContext(Dispatchers.IO) {
            val text = contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
                .trim()
            if (text.isEmpty()) throw Exception("文件为空")

            val root = JSONObject(text)
            val entities = mutableListOf<PhraseOverrideEntity>()
            var phraseCount = 0
            val keys = root.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                val ch = key.trim().take(1)
                if (ch.isEmpty()) continue
                val arr = root.optJSONArray(key) ?: continue
                val phrases = buildList(arr.length()) {
                    for (i in 0 until arr.length()) {
                        val phrase = arr.optString(i).orEmpty().trim()
                        if (phrase.isEmpty()) continue
                        if (phrase.length > 5) throw IllegalArgumentException("短语长度超过5字：$ch -> $phrase")
                        add(phrase)
                    }
                }.distinct()

                phraseCount += phrases.size
                entities.add(PhraseOverrideEntity(char = ch, phrasesJson = JSONArray(phrases).toString()))
            }

            for (entity in entities) phraseOverrideDao.upsert(entity)
            PhraseImportResult(
                importedChars = entities.size,
                importedPhrases = phraseCount,
            )
        }
    }
}
