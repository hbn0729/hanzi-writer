package com.hanzi.learner.app

import android.content.Context
import com.hanzi.learner.features.common.ports.CharacterRepositoryProvider
import com.hanzi.learner.character-writer.data.CharacterRepositoryFactory

internal class DefaultCharacterRepositoryProvider(
    private val context: Context,
    private val factory: CharacterRepositoryFactory,
) : CharacterRepositoryProvider {
    override fun get(useExternalDataset: Boolean) = factory.create(context, useExternalDataset)
}
