package com.hanzi.learner.features.admin.ui

import com.hanzi.learner.data.model.TimeProvider
import com.hanzi.learner.data.repository.TtsPreferenceRepositoryContract
import com.hanzi.learner.features.admin.domain.LoadAdminDashboardUseCase
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
import com.hanzi.learner.features.admin.viewmodel.AdminBackupViewModel
import com.hanzi.learner.features.admin.viewmodel.AdminCharacterViewModel
import com.hanzi.learner.features.admin.viewmodel.AdminDashboardViewModel
import com.hanzi.learner.features.admin.viewmodel.AdminLearningDataViewModel
import com.hanzi.learner.features.admin.viewmodel.AdminSettingsViewModel
import com.hanzi.learner.features.admin.viewmodel.AdminTtsViewModel
import com.hanzi.learner.app.AdminFeatureDependencies
import com.hanzi.learner.speech.contract.PreviewAudioPlayerContract
import com.hanzi.learner.speech.contract.TtsModelDownloadManagerContract
import com.hanzi.learner.speech.contract.TtsModelRepositoryContract
import com.hanzi.learner.speech.contract.TtsSpeakerContract

class AdminViewModelFactories(
    private val timeProvider: TimeProvider,
    private val indexRepository: AdminIndexRepository,
    private val appSettingsRepository: AdminAppSettingsRepository,
    private val disabledCharRepository: AdminDisabledCharRepository,
    private val progressQueryRepository: AdminProgressQueryRepository,
    private val progressCommandRepository: AdminProgressCommandRepository,
    private val phraseOverrideRepository: AdminPhraseOverrideRepository,
    private val backupDataTransferPort: BackupDataTransferPort,
    private val phraseImportPort: PhraseImportPort,
    private val curriculumImportPort: CurriculumImportPort,
    private val strokeImportPort: StrokeImportPort,
    private val ttsPreferenceRepository: TtsPreferenceRepositoryContract,
    private val ttsDownloadManager: TtsModelDownloadManagerContract,
    private val previewAudioPlayer: PreviewAudioPlayerContract,
    private val ttsSpeaker: TtsSpeakerContract,
    private val modelRepository: TtsModelRepositoryContract,
) {
    fun dashboardFactory(): AdminDashboardViewModel.Factory = AdminDashboardViewModel.Factory(
        progressCommandRepository = progressCommandRepository,
        timeProvider = timeProvider,
        loadDashboardUseCase = LoadAdminDashboardUseCase(
            indexRepository = indexRepository,
            appSettingsRepository = appSettingsRepository,
            disabledCharRepository = disabledCharRepository,
            progressRepository = progressQueryRepository,
            phraseOverrideRepository = phraseOverrideRepository,
        ),
    )

    fun characterFactory(): AdminCharacterViewModel.Factory = AdminCharacterViewModel.Factory(
        indexRepository = indexRepository,
        progressQueryRepository = progressQueryRepository,
        progressCommandRepository = progressCommandRepository,
        phraseOverrideRepository = phraseOverrideRepository,
        disabledCharRepository = disabledCharRepository,
        timeProvider = timeProvider,
    )

    fun learningFactory(): AdminLearningDataViewModel.Factory = AdminLearningDataViewModel.Factory(
        indexRepository = indexRepository,
        progressQueryRepository = progressQueryRepository,
        progressCommandRepository = progressCommandRepository,
        phraseOverrideRepository = phraseOverrideRepository,
        disabledCharRepository = disabledCharRepository,
    )

    fun settingsFactory(): AdminSettingsViewModel.Factory = AdminSettingsViewModel.Factory(appSettingsRepository)

    fun backupFactory(onDataChanged: () -> Unit): AdminBackupViewModel.Factory = AdminBackupViewModel.Factory(
        dataTransferPort = backupDataTransferPort,
        phraseImportPort = phraseImportPort,
        curriculumImportPort = curriculumImportPort,
        strokeImportPort = strokeImportPort,
        onDataChanged = onDataChanged,
    )

    fun ttsModelFactory(): AdminTtsViewModel.Factory = AdminTtsViewModel.Factory(
        preferenceRepository = ttsPreferenceRepository,
        downloadManager = ttsDownloadManager,
        previewPlayer = previewAudioPlayer,
        ttsSpeaker = ttsSpeaker,
        modelRepository = modelRepository,
    )

    companion object {
        fun from(deps: AdminFeatureDependencies): AdminViewModelFactories = AdminViewModelFactories(
            timeProvider = deps.timeProvider,
            indexRepository = deps.adminIndexRepository,
            appSettingsRepository = deps.adminAppSettingsRepository,
            disabledCharRepository = deps.adminDisabledCharRepository,
            progressQueryRepository = deps.adminProgressQueryRepository,
            progressCommandRepository = deps.adminProgressCommandRepository,
            phraseOverrideRepository = deps.adminPhraseOverrideRepository,
            backupDataTransferPort = deps.backupDataTransferPort,
            phraseImportPort = deps.phraseImportPort,
            curriculumImportPort = deps.curriculumImportPort,
            strokeImportPort = deps.strokeImportPort,
            ttsPreferenceRepository = deps.ttsPreferenceRepository,
            ttsDownloadManager = deps.ttsDownloadManager,
            previewAudioPlayer = deps.previewAudioPlayer,
            ttsSpeaker = deps.ttsSpeaker,
            modelRepository = deps.modelRepository,
        )
    }
}
