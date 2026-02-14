package com.hanzi.learner.ui

import android.content.Context
import com.hanzi.learner.db.AppSettingsRepositoryContract
import com.hanzi.learner.db.DisabledCharRepositoryContract
import com.hanzi.learner.db.ProgressRepositoryContract
import com.hanzi.learner.db.TimeProvider
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

    override val homeDeps: HomeFeatureDependencies = this
    override val practiceDeps: PracticeFeatureDependencies = this
    override val adminDeps: AdminFeatureDependencies = this
}
