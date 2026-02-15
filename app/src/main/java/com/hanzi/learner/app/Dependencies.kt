package com.hanzi.learner.app

import com.hanzi.learner.data.repository.AppSettingsRepositoryContract
import com.hanzi.learner.data.repository.DisabledCharRepositoryContract
import com.hanzi.learner.data.repository.ProgressRepositoryContract
import com.hanzi.learner.data.model.TimeProvider
import com.hanzi.learner.features.admin.domain.AdminCharacterDataLoader
import com.hanzi.learner.features.admin.domain.AdminDashboardDataLoader
import com.hanzi.learner.features.admin.domain.AdminIndexDataLoader
import com.hanzi.learner.features.admin.domain.AdminLearningDataLoader
import com.hanzi.learner.features.admin.repository.AdminAppSettingsRepository
import com.hanzi.learner.features.admin.repository.AdminDisabledCharRepository
import com.hanzi.learner.features.admin.repository.AdminIndexRepository
import com.hanzi.learner.features.admin.repository.AdminPhraseOverrideRepository
import com.hanzi.learner.features.admin.repository.AdminProgressCommandRepository
import com.hanzi.learner.features.admin.repository.AdminProgressQueryRepository
import com.hanzi.learner.features.admin.repository.BackupDataTransferPort
import com.hanzi.learner.features.admin.repository.CurriculumImportPort
import com.hanzi.learner.features.admin.repository.PhraseImportPort
import com.hanzi.learner.features.admin.repository.StrokeImportPort
import com.hanzi.learner.features.common.ports.CharacterRepositoryProvider
import com.hanzi.learner.features.practice.domain.CompletePracticeCharacterUseCase
import com.hanzi.learner.features.practice.domain.PracticeSessionEngineFactory
import com.hanzi.learner.character-writer.match.StrokeMatcherContract

interface HomeFeatureDependencies {
    val progressRepository: ProgressRepositoryContract
    val appSettingsRepository: AppSettingsRepositoryContract
    val disabledCharRepository: DisabledCharRepositoryContract
    val characterRepositoryProvider: CharacterRepositoryProvider
}

interface PracticeFeatureDependencies {
    val practiceSessionEngineFactory: PracticeSessionEngineFactory
    val completePracticeCharacterUseCase: CompletePracticeCharacterUseCase
    val strokeMatcher: StrokeMatcherContract
}

interface AdminFeatureDependencies {
    val timeProvider: TimeProvider
    val adminIndexRepository: AdminIndexRepository
    val adminAppSettingsRepository: AdminAppSettingsRepository
    val adminDisabledCharRepository: AdminDisabledCharRepository
    val adminProgressQueryRepository: AdminProgressQueryRepository
    val adminProgressCommandRepository: AdminProgressCommandRepository
    val adminPhraseOverrideRepository: AdminPhraseOverrideRepository
    val backupDataTransferPort: BackupDataTransferPort
    val phraseImportPort: PhraseImportPort
    val curriculumImportPort: CurriculumImportPort
    val strokeImportPort: StrokeImportPort
    val adminIndexDataLoader: AdminIndexDataLoader
    val adminDashboardDataLoader: AdminDashboardDataLoader
    val adminCharacterDataLoader: AdminCharacterDataLoader
    val adminLearningDataLoader: AdminLearningDataLoader
}

interface AppDependencies {
    val homeDeps: HomeFeatureDependencies
    val practiceDeps: PracticeFeatureDependencies
    val adminDeps: AdminFeatureDependencies
}
