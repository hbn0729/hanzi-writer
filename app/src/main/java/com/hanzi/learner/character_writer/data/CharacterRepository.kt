package com.hanzi.learner.character_writer.data

import com.hanzi.learner.character_writer.model.CharacterData

interface CharacterRepository {
    suspend fun loadIndex(): List<CharIndexItem>

    suspend fun loadCharacter(item: CharIndexItem): CharacterData
}
