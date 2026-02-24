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
    private val adminModule: AdminModuleApi by lazy {
        AdminModule(
            context = context,
            coreDataModule = coreDataModule,
            characterRepositoryProvider = practiceModule.characterRepositoryProvider,
            cacheController = practiceModule.characterCacheController,
        )
    }

    override val progressRepository: ProgressRepositoryContract = coreDataModule.progressRepository
    override val appSettingsRepository: AppSettingsRepositoryContract = coreDataModule.appSettingsRepository
    override val disabledCharRepository: DisabledCharRepositoryContract = coreDataModule.disabledCharRepository

    override val characterRepositoryProvider: CharacterRepositoryProvider = practiceModule.characterRepositoryProvider
    override val practiceSessionEngineFactory: PracticeSessionEngineFactory = practiceModule.practiceSessionEngineFactory
    override val completePracticeCharacterUseCase: CompletePracticeCharacterUseCase =
        practiceModule.completePracticeCharacterUseCase
    override val strokeMatcher: StrokeMatcherContract = practiceModule.strokeMatcher

    override val timeProvider: TimeProvider = coreDataModule.timeProvider
    override val adminIndexRepository: AdminIndexRepository get() = adminModule.adminIndexRepository
    override val adminAppSettingsRepository: AdminAppSettingsRepository get() = adminModule.adminAppSettingsRepository
    override val adminDisabledCharRepository: AdminDisabledCharRepository get() = adminModule.adminDisabledCharRepository
    override val adminProgressQueryRepository: AdminProgressQueryRepository get() = adminModule.adminProgressQueryRepository
    override val adminProgressCommandRepository: AdminProgressCommandRepository get() = adminModule.adminProgressCommandRepository
    override val adminPhraseOverrideRepository: AdminPhraseOverrideRepository get() = adminModule.adminPhraseOverrideRepository
    override val backupDataTransferPort: BackupDataTransferPort get() = adminModule.backupDataTransferPort
    override val phraseImportPort: PhraseImportPort get() = adminModule.phraseImportPort
    override val curriculumImportPort: CurriculumImportPort get() = adminModule.curriculumImportPort
    override val strokeImportPort: StrokeImportPort get() = adminModule.strokeImportPort
    override val adminIndexDataLoader: AdminIndexDataLoader get() = adminModule.adminIndexDataLoader
    override val adminDashboardDataLoader: AdminDashboardDataLoader get() = adminModule.adminDashboardDataLoader
    override val adminCharacterDataLoader: AdminCharacterDataLoader get() = adminModule.adminCharacterDataLoader
    override val adminLearningDataLoader: AdminLearningDataLoader get() = adminModule.adminLearningDataLoader

    override val homeDeps: HomeFeatureDependencies = this
    override val practiceDeps: PracticeFeatureDependencies = this
    override val adminDeps: AdminFeatureDependencies = this
}
