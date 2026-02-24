package com.hanzi.learner.app

import android.content.Context
import com.hanzi.learner.character_writer.data.CharacterRepository
import com.hanzi.learner.features.common.ports.CharacterCacheController
import com.hanzi.learner.features.common.ports.CharacterRepositoryProvider
import com.hanzi.learner.character_writer.data.CharacterRepositoryFactory

internal class DefaultCharacterRepositoryProvider(
    private val context: Context,
    private val factory: CharacterRepositoryFactory,
) : CharacterRepositoryProvider, CharacterCacheController {
    private var cachedRepo: CharacterRepository? = null
    private var cachedUseExternal: Boolean? = null

    override fun get(useExternalDataset: Boolean): CharacterRepository {
        if (useExternalDataset == cachedUseExternal) {
            cachedRepo?.let { return it }
        }
        return factory.create(context, useExternalDataset).also {
            cachedRepo = it
            cachedUseExternal = useExternalDataset
        }
    }

    override fun invalidate() {
        cachedRepo = null
        cachedUseExternal = null
    }
}
