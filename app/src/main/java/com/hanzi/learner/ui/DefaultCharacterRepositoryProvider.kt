package com.hanzi.learner.ui

import android.content.Context
import com.hanzi.learner.feature.common.ports.CharacterRepositoryProvider
import com.hanzi.learner.hanzi.data.CharacterRepositoryFactory

internal class DefaultCharacterRepositoryProvider(
    private val context: Context,
    private val factory: CharacterRepositoryFactory,
) : CharacterRepositoryProvider {
    override fun get(useExternalDataset: Boolean) = factory.create(context, useExternalDataset)
}
