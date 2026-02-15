package com.hanzi.learner.character-writer.data

import android.content.Context
import java.io.File

interface CharacterRepositoryFactory {
    fun create(context: Context, useExternalDataset: Boolean): CharacterRepository
}

fun interface CharacterRepositorySource {
    fun create(context: Context, useExternalDataset: Boolean): CharacterRepository?
}

object DefaultCharacterRepositoryFactory : CharacterRepositoryFactory {
    private val sources: List<CharacterRepositorySource> = listOf(
        ExternalDatasetRepositorySource,
        AssetFallbackRepositorySource,
    )

    override fun create(context: Context, useExternalDataset: Boolean): CharacterRepository {
        return CharacterRepositorySelector.select(context, useExternalDataset, sources)
    }
}

object CharacterRepositorySelector {
    fun externalDatasetDir(context: Context): File {
        return File(context.filesDir, "hanzi_dataset")
    }

    internal fun select(
        context: Context,
        useExternalDataset: Boolean,
        sources: List<CharacterRepositorySource>,
    ): CharacterRepository {
        for (source in sources) {
            val repository = source.create(context, useExternalDataset)
            if (repository != null) {
                return repository
            }
        }
        return AssetCharacterRepository(context)
    }
}

object ExternalDatasetRepositorySource : CharacterRepositorySource {
    override fun create(context: Context, useExternalDataset: Boolean): CharacterRepository? {
        if (!useExternalDataset) return null

        val dir = CharacterRepositorySelector.externalDatasetDir(context)
        val index = File(dir, "char_index.json")
        val charDataDir = File(dir, "char_data")
        if (index.exists() && index.length() > 2 && charDataDir.isDirectory) {
            return FileCharacterRepository(dir)
        }
        return null
    }
}

object AssetFallbackRepositorySource : CharacterRepositorySource {
    override fun create(context: Context, useExternalDataset: Boolean): CharacterRepository {
        return AssetCharacterRepository(context)
    }
}
