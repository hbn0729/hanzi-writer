package com.hanzi.learner.feature.common.ports

import com.hanzi.learner.hanzi.data.CharacterRepository

interface CharacterRepositoryProvider {
    fun get(useExternalDataset: Boolean): CharacterRepository
}
