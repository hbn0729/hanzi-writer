package com.hanzi.learner.character_writer.data

import android.util.LruCache
import com.hanzi.learner.character_writer.model.CharacterData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileCharacterRepository(
    private val baseDir: File,
) : CharacterRepository {
    private var indexCache: List<CharIndexItem>? = null
    private val characterCache = LruCache<String, CharacterData>(40)

    override suspend fun loadIndex(): List<CharIndexItem> {
        indexCache?.let { return it }
        return withContext(Dispatchers.IO) {
            val indexFile = File(baseDir, "char_index.json")
            val json = indexFile.readText(Charsets.UTF_8)
            CharacterJsonParser.parseIndex(json).also { indexCache = it }
        }
    }

    override suspend fun loadCharacter(item: CharIndexItem): CharacterData {
        characterCache.get(item.char)?.let { return it }
        return withContext(Dispatchers.IO) {
            val json = File(baseDir, item.file).readText(Charsets.UTF_8)
            CharacterJsonParser.parseCharacter(item, json).also { characterCache.put(item.char, it) }
        }
    }
}
