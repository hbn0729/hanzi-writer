package com.hanzi.learner.features.home.viewmodel

import com.hanzi.learner.data.repository.AppSettingsRepositoryContract
import com.hanzi.learner.data.repository.DisabledCharRepositoryContract
import com.hanzi.learner.data.repository.ProgressRepositoryContract
import com.hanzi.learner.features.common.ports.CharacterRepositoryProvider
import com.hanzi.learner.features.home.domain.LoadHomeDataUseCase
import com.hanzi.learner.features.home.domain.ResolveHomeCharacterRepositoryUseCase

object HomeViewModelFactoryBuilder {
    fun fromDependencies(
        progressRepository: ProgressRepositoryContract,
        appSettingsRepository: AppSettingsRepositoryContract,
        disabledCharRepository: DisabledCharRepositoryContract,
        characterRepositoryProvider: CharacterRepositoryProvider,
        navigationCallback: (HomeNavigation) -> Unit,
    ): HomeViewModel.Factory {
        val resolveCharacterRepositoryUseCase = ResolveHomeCharacterRepositoryUseCase(
            appSettingsRepository = appSettingsRepository,
            characterRepositoryProvider = characterRepositoryProvider,
        )
        val loadHomeDataUseCase = LoadHomeDataUseCase(
            progressRepository = progressRepository,
            disabledCharRepository = disabledCharRepository,
            resolveCharacterRepositoryUseCase = resolveCharacterRepositoryUseCase,
        )
        return HomeViewModel.Factory(
            loadHomeDataUseCase = loadHomeDataUseCase,
            navigationCallback = navigationCallback,
        )
    }
}
