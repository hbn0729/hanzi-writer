package com.hanzi.learner.character_writer.data

import android.content.Context
import android.util.LruCache
import com.hanzi.learner.character_writer.model.CharacterData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssetCharacterRepository(
    private val context: Context,
) : CharacterRepository {
    private var indexCache: List<CharIndexItem>? = null
    private val characterCache = LruCache<String, CharacterData>(40)

    override suspend fun loadIndex(): List<CharIndexItem> {
        indexCache?.let { return it }
        return withContext(Dispatchers.IO) {
            val json = context.assets.open("char_index.json").bufferedReader().use { it.readText() }
            CharacterJsonParser.parseIndex(json).also { indexCache = it }
        }
    }

    override suspend fun loadCharacter(item: CharIndexItem): CharacterData {
        characterCache.get(item.char)?.let { return it }
        return withContext(Dispatchers.IO) {
            val json = context.assets.open(item.file).bufferedReader().use { it.readText() }
            CharacterJsonParser.parseCharacter(item, json).also { characterCache.put(item.char, it) }
        }
    }
}
