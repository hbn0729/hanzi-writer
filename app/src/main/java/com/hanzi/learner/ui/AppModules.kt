package com.hanzi.learner.ui

import android.content.Context
import com.hanzi.learner.db.AppDatabase
import com.hanzi.learner.db.AppSettingsRepository
import com.hanzi.learner.db.AppSettingsRepositoryContract
import com.hanzi.learner.db.BackupManager
import com.hanzi.learner.db.BackupRepository
import com.hanzi.learner.db.BackupRepositoryContract
import com.hanzi.learner.db.BackupSerializer
import com.hanzi.learner.db.DefaultSpacedRepetitionPolicy
import com.hanzi.learner.db.DisabledCharRepository
import com.hanzi.learner.db.DisabledCharRepositoryContract
import com.hanzi.learner.db.PhraseOverrideRepository
import com.hanzi.learner.db.PhraseOverrideRepositoryContract
import com.hanzi.learner.db.ProgressRepository
import com.hanzi.learner.db.ProgressRepositoryContract
import com.hanzi.learner.db.SystemTimeProvider
import com.hanzi.learner.db.TimeProvider
import com.hanzi.learner.feature.admin.backup.BackupZipExtractor
import com.hanzi.learner.feature.admin.backup.StrokeDatasetParser
import com.hanzi.learner.feature.admin.backup.StrokeDatasetWriter
import com.hanzi.learner.feature.admin.domain.AdminCharacterDataLoader
import com.hanzi.learner.feature.admin.domain.AdminCharacterDataLoaderImpl
import com.hanzi.learner.feature.admin.domain.AdminDashboardDataLoader
import com.hanzi.learner.feature.admin.domain.AdminDashboardDataLoaderImpl
import com.hanzi.learner.feature.admin.domain.AdminIndexDataLoader
import com.hanzi.learner.feature.admin.domain.AdminIndexDataLoaderImpl
import com.hanzi.learner.feature.admin.domain.AdminLearningDataLoader
import com.hanzi.learner.feature.admin.domain.AdminLearningDataLoaderImpl
import com.hanzi.learner.feature.admin.repository.AdminAppSettingsRepository
import com.hanzi.learner.feature.admin.repository.AdminAppSettingsRepositoryImpl
import com.hanzi.learner.feature.admin.repository.AdminBackupDataTransferPortAdapter
import com.hanzi.learner.feature.admin.repository.AdminCurriculumImportPortAdapter
import com.hanzi.learner.feature.admin.repository.AdminDisabledCharRepository
import com.hanzi.learner.feature.admin.repository.AdminDisabledCharRepositoryImpl
import com.hanzi.learner.feature.admin.repository.AdminIndexRepository
import com.hanzi.learner.feature.admin.repository.AdminIndexRepositoryImpl
import com.hanzi.learner.feature.admin.repository.AdminPhraseImportPortAdapter
import com.hanzi.learner.feature.admin.repository.AdminPhraseOverrideRepository
import com.hanzi.learner.feature.admin.repository.AdminPhraseOverrideRepositoryImpl
import com.hanzi.learner.feature.admin.repository.AdminProgressCommandRepository
import com.hanzi.learner.feature.admin.repository.AdminProgressQueryRepository
import com.hanzi.learner.feature.admin.repository.AdminProgressRepositoryImpl
import com.hanzi.learner.feature.admin.repository.AdminStrokeImportPortAdapter
import com.hanzi.learner.feature.admin.repository.BackupDataTransferPort
import com.hanzi.learner.feature.admin.repository.CurriculumImportPort
import com.hanzi.learner.feature.admin.repository.PhraseImportPort
import com.hanzi.learner.feature.admin.repository.StrokeImportPort
import com.hanzi.learner.feature.common.ports.CharacterRepositoryProvider
import com.hanzi.learner.feature.practice.domain.CompletePracticeCharacterUseCase
import com.hanzi.learner.feature.practice.domain.PickNextPracticeItemUseCase
import com.hanzi.learner.feature.practice.domain.PhraseOverridePracticePhraseProvider
import com.hanzi.learner.feature.practice.domain.PracticeSessionEngineFactory
import com.hanzi.learner.feature.practice.domain.PracticeSessionOrchestrator
import com.hanzi.learner.hanzi.data.CharacterRepositoryFactory
import com.hanzi.learner.hanzi.data.DefaultCharacterRepositoryFactory
import com.hanzi.learner.hanzi.match.DefaultStrokeMatcher
import com.hanzi.learner.hanzi.match.StrokeMatcherContract

internal interface CoreDataModuleDependencies {
    val database: AppDatabase
    val timeProvider: TimeProvider
    val progressRepository: ProgressRepositoryContract
    val appSettingsRepository: AppSettingsRepositoryContract
    val disabledCharRepository: DisabledCharRepositoryContract
    val phraseOverrideRepository: PhraseOverrideRepositoryContract
    val backupManager: BackupManager
}

internal interface PracticeModuleDependencies {
    val progressRepository: ProgressRepositoryContract
    val appSettingsRepository: AppSettingsRepositoryContract
    val disabledCharRepository: DisabledCharRepositoryContract
    val phraseOverrideRepository: PhraseOverrideRepositoryContract
}

internal interface AdminModuleDependencies {
    val database: AppDatabase
    val timeProvider: TimeProvider
    val backupManager: BackupManager
}

internal interface PracticeModuleApi {
    val characterRepositoryProvider: CharacterRepositoryProvider
    val practiceSessionEngineFactory: PracticeSessionEngineFactory
    val completePracticeCharacterUseCase: CompletePracticeCharacterUseCase
    val strokeMatcher: StrokeMatcherContract
}

internal interface AdminModuleApi {
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

internal class CoreDataModule(
    context: Context,
) : CoreDataModuleDependencies,
    PracticeModuleDependencies,
    AdminModuleDependencies {
    private val appContext = context.applicationContext
    override val database: AppDatabase = AppDatabase.getInstance(appContext)
    override val timeProvider: TimeProvider = SystemTimeProvider

    override val progressRepository: ProgressRepositoryContract = ProgressRepository(
        dao = database.hanziProgressDao(),
        timeProvider = timeProvider,
        policy = DefaultSpacedRepetitionPolicy,
    )
    override val appSettingsRepository: AppSettingsRepositoryContract = AppSettingsRepository(database.appSettingsDao())
    override val disabledCharRepository: DisabledCharRepositoryContract = DisabledCharRepository(database.disabledCharDao())
    override val phraseOverrideRepository: PhraseOverrideRepositoryContract = PhraseOverrideRepository(database.phraseOverrideDao())

    private val backupRepository: BackupRepositoryContract = BackupRepository(
        progressDao = database.hanziProgressDao(),
        phraseOverrideDao = database.phraseOverrideDao(),
        disabledCharDao = database.disabledCharDao(),
        appSettingsDao = database.appSettingsDao(),
    )

    override val backupManager = BackupManager(
        serializer = BackupSerializer(),
        repository = backupRepository,
    )
}

internal class PracticeModule(
    context: Context,
    coreDataModule: PracticeModuleDependencies,
) : PracticeModuleApi {
    private val characterRepositoryFactory: CharacterRepositoryFactory = DefaultCharacterRepositoryFactory
    override val characterRepositoryProvider: CharacterRepositoryProvider = DefaultCharacterRepositoryProvider(
        context = context.applicationContext,
        factory = characterRepositoryFactory,
    )
    private val itemSelector = PickNextPracticeItemUseCase(coreDataModule.progressRepository)
    private val phraseProvider = PhraseOverridePracticePhraseProvider(coreDataModule.phraseOverrideRepository)

    override val practiceSessionEngineFactory: PracticeSessionEngineFactory = PracticeSessionOrchestrator(
        appSettingsRepository = coreDataModule.appSettingsRepository,
        disabledCharRepository = coreDataModule.disabledCharRepository,
        characterRepositoryProvider = characterRepositoryProvider,
        itemSelector = itemSelector,
        phraseProvider = phraseProvider,
    )
    override val completePracticeCharacterUseCase = CompletePracticeCharacterUseCase(coreDataModule.progressRepository)

    override val strokeMatcher: StrokeMatcherContract = DefaultStrokeMatcher
}

internal class AdminModule(
    context: Context,
    coreDataModule: AdminModuleDependencies,
    characterRepositoryProvider: CharacterRepositoryProvider,
) : AdminModuleApi {
    private val appContext = context.applicationContext
    private val database = coreDataModule.database

    private val backupZipExtractor = BackupZipExtractor(appContext.contentResolver)
    private val strokeDatasetParser = StrokeDatasetParser()
    private val strokeDatasetWriter = StrokeDatasetWriter()

    override val adminIndexRepository: AdminIndexRepository = AdminIndexRepositoryImpl(characterRepositoryProvider, database.appSettingsDao())
    override val adminAppSettingsRepository: AdminAppSettingsRepository = AdminAppSettingsRepositoryImpl(database.appSettingsDao())
    override val adminDisabledCharRepository: AdminDisabledCharRepository = AdminDisabledCharRepositoryImpl(database.disabledCharDao())
    private val adminProgressRepositoryImpl = AdminProgressRepositoryImpl(
        hanziProgressDao = database.hanziProgressDao(),
        timeProvider = coreDataModule.timeProvider,
    )
    override val adminProgressQueryRepository: AdminProgressQueryRepository = adminProgressRepositoryImpl
    override val adminProgressCommandRepository: AdminProgressCommandRepository = adminProgressRepositoryImpl
    override val adminPhraseOverrideRepository: AdminPhraseOverrideRepository = AdminPhraseOverrideRepositoryImpl(database.phraseOverrideDao())
    override val backupDataTransferPort: BackupDataTransferPort = AdminBackupDataTransferPortAdapter(
        contentResolver = appContext.contentResolver,
        backupManager = coreDataModule.backupManager,
    )
    override val phraseImportPort: PhraseImportPort = AdminPhraseImportPortAdapter(
        contentResolver = appContext.contentResolver,
        phraseOverrideDao = database.phraseOverrideDao(),
    )
    override val curriculumImportPort: CurriculumImportPort = AdminCurriculumImportPortAdapter(
        contentResolver = appContext.contentResolver,
        disabledCharDao = database.disabledCharDao(),
    )
    override val strokeImportPort: StrokeImportPort = AdminStrokeImportPortAdapter(
        contentResolver = appContext.contentResolver,
        filesDir = appContext.filesDir,
        cacheDir = appContext.cacheDir,
        appSettingsDao = database.appSettingsDao(),
        backupZipExtractor = backupZipExtractor,
        strokeDatasetParser = strokeDatasetParser,
        strokeDatasetWriter = strokeDatasetWriter,
    )

    override val adminIndexDataLoader: AdminIndexDataLoader = AdminIndexDataLoaderImpl(
        indexRepository = adminIndexRepository,
    )

    override val adminDashboardDataLoader: AdminDashboardDataLoader = AdminDashboardDataLoaderImpl(
        indexRepository = adminIndexRepository,
        appSettingsRepository = adminAppSettingsRepository,
        disabledCharRepository = adminDisabledCharRepository,
        progressQueryRepository = adminProgressQueryRepository,
        phraseOverrideRepository = adminPhraseOverrideRepository,
        timeProvider = coreDataModule.timeProvider,
    )

    override val adminCharacterDataLoader: AdminCharacterDataLoader = AdminCharacterDataLoaderImpl(
        indexRepository = adminIndexRepository,
        disabledCharRepository = adminDisabledCharRepository,
        progressQueryRepository = adminProgressQueryRepository,
    )

    override val adminLearningDataLoader: AdminLearningDataLoader = AdminLearningDataLoaderImpl(
        indexRepository = adminIndexRepository,
        progressQueryRepository = adminProgressQueryRepository,
        disabledCharRepository = adminDisabledCharRepository,
    )
}
