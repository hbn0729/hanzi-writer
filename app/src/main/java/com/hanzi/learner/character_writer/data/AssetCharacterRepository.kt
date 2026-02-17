package com.hanzi.learner.character_writer.data

import android.content.Context
import com.hanzi.learner.character_writer.model.CharacterData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssetCharacterRepository(
    private val context: Context,
) : CharacterRepository {
    override suspend fun loadIndex(): List<CharIndexItem> = withContext(Dispatchers.IO) {
        val json = context.assets.open("char_index.json").bufferedReader().use { it.readText() }
        CharacterJsonParser.parseIndex(json)
    }

    override suspend fun loadCharacter(item: CharIndexItem): CharacterData = withContext(Dispatchers.IO) {
        val json = context.assets.open(item.file).bufferedReader().use { it.readText() }
        CharacterJsonParser.parseCharacter(item, json)
    }
}
