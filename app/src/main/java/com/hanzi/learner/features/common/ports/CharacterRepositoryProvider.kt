package com.hanzi.learner.features.common.ports

import com.hanzi.learner.character_writer.data.CharacterRepository

interface CharacterRepositoryProvider {
    fun get(useExternalDataset: Boolean): CharacterRepository
}
