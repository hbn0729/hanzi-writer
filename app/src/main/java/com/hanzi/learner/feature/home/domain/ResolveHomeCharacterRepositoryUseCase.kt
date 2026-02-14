package com.hanzi.learner.feature.home.domain

import com.hanzi.learner.db.AppSettingsRepositoryContract
import com.hanzi.learner.feature.common.ports.CharacterRepositoryProvider
import com.hanzi.learner.hanzi.data.CharacterRepository

class ResolveHomeCharacterRepositoryUseCase(
    private val appSettingsRepository: AppSettingsRepositoryContract,
    private val characterRepositoryProvider: CharacterRepositoryProvider,
) {
    suspend operator fun invoke(): CharacterRepository {
        val settings = appSettingsRepository.getSettings()
        return characterRepositoryProvider.get(settings.useExternalDataset)
    }
}
