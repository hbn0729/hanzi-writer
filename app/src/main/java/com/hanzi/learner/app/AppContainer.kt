package com.hanzi.learner.app

import android.content.Context
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
import com.hanzi.learner.character_writer.match.StrokeMatcherContract

class AppContainer(
    context: Context,
) : AppDependencies,
    HomeFeatureDependencies,
    PracticeFeatureDependencies,
    AdminFeatureDependencies {
    private val coreDataModule = CoreDataModule(context)
    private val practiceModule: PracticeModuleApi = PracticeModule(
        context = context,
        coreDataModule = coreDataModule,
    )
    private val adminModule: AdminModuleApi = AdminModule(
        context = context,
        coreDataModule = coreDataModule,
        characterRepositoryProvider = practiceModule.characterRepositoryProvider,
    )

    override val progressRepository: ProgressRepositoryContract = coreDataModule.progressRepository
    override val appSettingsRepository: AppSettingsRepositoryContract = coreDataModule.appSettingsRepository
    override val disabledCharRepository: DisabledCharRepositoryContract = coreDataModule.disabledCharRepository

    override val characterRepositoryProvider: CharacterRepositoryProvider = practiceModule.characterRepositoryProvider
    override val practiceSessionEngineFactory: PracticeSessionEngineFactory = practiceModule.practiceSessionEngineFactory
    override val completePracticeCharacterUseCase: CompletePracticeCharacterUseCase =
        practiceModule.completePracticeCharacterUseCase
    override val strokeMatcher: StrokeMatcherContract = practiceModule.strokeMatcher

    override val timeProvider: TimeProvider = coreDataModule.timeProvider
    override val adminIndexRepository: AdminIndexRepository = adminModule.adminIndexRepository
    override val adminAppSettingsRepository: AdminAppSettingsRepository = adminModule.adminAppSettingsRepository
    override val adminDisabledCharRepository: AdminDisabledCharRepository = adminModule.adminDisabledCharRepository
    override val adminProgressQueryRepository: AdminProgressQueryRepository = adminModule.adminProgressQueryRepository
    override val adminProgressCommandRepository: AdminProgressCommandRepository = adminModule.adminProgressCommandRepository
    override val adminPhraseOverrideRepository: AdminPhraseOverrideRepository = adminModule.adminPhraseOverrideRepository
    override val backupDataTransferPort: BackupDataTransferPort = adminModule.backupDataTransferPort
    override val phraseImportPort: PhraseImportPort = adminModule.phraseImportPort
    override val curriculumImportPort: CurriculumImportPort = adminModule.curriculumImportPort
    override val strokeImportPort: StrokeImportPort = adminModule.strokeImportPort
    override val adminIndexDataLoader: AdminIndexDataLoader = adminModule.adminIndexDataLoader
    override val adminDashboardDataLoader: AdminDashboardDataLoader = adminModule.adminDashboardDataLoader
    override val adminCharacterDataLoader: AdminCharacterDataLoader = adminModule.adminCharacterDataLoader
    override val adminLearningDataLoader: AdminLearningDataLoader = adminModule.adminLearningDataLoader

    override val homeDeps: HomeFeatureDependencies = this
    override val practiceDeps: PracticeFeatureDependencies = this
    override val adminDeps: AdminFeatureDependencies = this
}
