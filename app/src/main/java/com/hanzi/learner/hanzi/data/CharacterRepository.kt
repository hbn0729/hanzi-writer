package com.hanzi.learner.hanzi.data

import com.hanzi.learner.hanzi.model.CharacterData

interface CharacterRepository {
    suspend fun loadIndex(): List<CharIndexItem>

    suspend fun loadCharacter(item: CharIndexItem): CharacterData
}
