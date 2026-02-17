package com.hanzi.learner.features.home.domain

import com.hanzi.learner.data.repository.AppSettingsRepositoryContract
import com.hanzi.learner.features.common.ports.CharacterRepositoryProvider
import com.hanzi.learner.character_writer.data.CharacterRepository

class ResolveHomeCharacterRepositoryUseCase(
    private val appSettingsRepository: AppSettingsRepositoryContract,
    private val characterRepositoryProvider: CharacterRepositoryProvider,
) {
    suspend operator fun invoke(): CharacterRepository {
        val settings = appSettingsRepository.getSettings()
        return characterRepositoryProvider.get(settings.useExternalDataset)
    }
}
