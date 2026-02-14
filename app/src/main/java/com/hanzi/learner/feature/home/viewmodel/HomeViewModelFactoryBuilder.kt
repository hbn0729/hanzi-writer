package com.hanzi.learner.feature.home.viewmodel

import com.hanzi.learner.db.AppSettingsRepositoryContract
import com.hanzi.learner.db.DisabledCharRepositoryContract
import com.hanzi.learner.db.ProgressRepositoryContract
import com.hanzi.learner.feature.common.ports.CharacterRepositoryProvider
import com.hanzi.learner.feature.home.domain.LoadHomeDataUseCase
import com.hanzi.learner.feature.home.domain.ResolveHomeCharacterRepositoryUseCase

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
