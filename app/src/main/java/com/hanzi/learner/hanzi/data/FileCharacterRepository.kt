package com.hanzi.learner.hanzi.data

import com.hanzi.learner.hanzi.model.CharacterData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileCharacterRepository(
    private val baseDir: File,
) : CharacterRepository {
    override suspend fun loadIndex(): List<CharIndexItem> = withContext(Dispatchers.IO) {
        val indexFile = File(baseDir, "char_index.json")
        val json = indexFile.readText(Charsets.UTF_8)
        CharacterJsonParser.parseIndex(json)
    }

    override suspend fun loadCharacter(item: CharIndexItem): CharacterData = withContext(Dispatchers.IO) {
        val json = File(baseDir, item.file).readText(Charsets.UTF_8)
        CharacterJsonParser.parseCharacter(item, json)
    }
}
