package com.hanzi.learner.ui

import com.hanzi.learner.db.AppSettingsRepositoryContract
import com.hanzi.learner.db.DisabledCharRepositoryContract
import com.hanzi.learner.db.ProgressRepositoryContract
import com.hanzi.learner.db.TimeProvider
import com.hanzi.learner.feature.admin.domain.AdminCharacterDataLoader
import com.hanzi.learner.feature.admin.domain.AdminDashboardDataLoader
import com.hanzi.learner.feature.admin.domain.AdminIndexDataLoader
import com.hanzi.learner.feature.admin.domain.AdminLearningDataLoader
import com.hanzi.learner.feature.admin.repository.AdminAppSettingsRepository
import com.hanzi.learner.feature.admin.repository.AdminDisabledCharRepository
import com.hanzi.learner.feature.admin.repository.AdminIndexRepository
import com.hanzi.learner.feature.admin.repository.AdminPhraseOverrideRepository
import com.hanzi.learner.feature.admin.repository.AdminProgressCommandRepository
import com.hanzi.learner.feature.admin.repository.AdminProgressQueryRepository
import com.hanzi.learner.feature.admin.repository.BackupDataTransferPort
import com.hanzi.learner.feature.admin.repository.CurriculumImportPort
import com.hanzi.learner.feature.admin.repository.PhraseImportPort
import com.hanzi.learner.feature.admin.repository.StrokeImportPort
import com.hanzi.learner.feature.common.ports.CharacterRepositoryProvider
import com.hanzi.learner.feature.practice.domain.CompletePracticeCharacterUseCase
import com.hanzi.learner.feature.practice.domain.PracticeSessionEngineFactory
import com.hanzi.learner.hanzi.match.StrokeMatcherContract

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
