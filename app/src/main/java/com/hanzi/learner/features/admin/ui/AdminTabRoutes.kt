package com.hanzi.learner.features.admin.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hanzi.learner.data.model.ImportMode
import com.hanzi.learner.features.admin.ui.tabs.BackupTab
import com.hanzi.learner.features.admin.ui.tabs.CharacterManagementTab
import com.hanzi.learner.features.admin.ui.tabs.LearningDataTab
import com.hanzi.learner.features.admin.ui.tabs.OverviewTab
import com.hanzi.learner.features.admin.ui.tabs.SettingsTab
import com.hanzi.learner.features.admin.viewmodel.AdminBackupViewModel
import com.hanzi.learner.features.admin.viewmodel.AdminCharacterViewModel
import com.hanzi.learner.features.admin.viewmodel.AdminDashboardViewModel
import com.hanzi.learner.features.admin.viewmodel.AdminLearningDataViewModel
import com.hanzi.learner.features.admin.viewmodel.AdminSettingsViewModel
import com.hanzi.learner.character_writer.data.CharIndexItem

@Composable
fun OverviewTabRoute(
    modifier: Modifier,
    onBack: () -> Unit,
    factories: AdminViewModelFactories,
    notifier: AdminDataChangedNotifier,
) {
    val factory = remember(factories) { factories.dashboardFactory() }
    val viewModel: AdminDashboardViewModel = viewModel(factory = factory, key = "admin_dashboard")
    val state by viewModel.uiState.collectAsState()
    val refreshVersion by notifier.version.collectAsState()

    LaunchedEffect(refreshVersion) {
        if (refreshVersion > 0) viewModel.loadDashboard()
    }

    OverviewTab(
        modifier = modifier,
        indexItems = state.indexItems,
        disabledChars = state.disabledChars,
        learnedCount = state.data?.learnedCount ?: 0,
        dueCount = state.data?.dueCount ?: 0,
        phraseOverrideCount = state.data?.phraseOverrideCount ?: 0,
        topWrong = state.data?.topWrong ?: emptyList(),
        dueProgress = state.data?.dueProgress ?: emptyList(),
        studyCounts = state.data?.studyCounts ?: emptyList(),
        onMarkDueToday = { viewModel.markDueToday(it) },
        onResetProgress = { viewModel.resetProgress(it) },
        onBack = onBack,
    )
}

@Composable
fun CharacterManagementTabRoute(
    modifier: Modifier,
    onBack: () -> Unit,
    factories: AdminViewModelFactories,
    notifier: AdminDataChangedNotifier,
) {
    val factory = remember(factories) { factories.characterFactory() }
    val viewModel: AdminCharacterViewModel = viewModel(factory = factory, key = "admin_character")
    val state by viewModel.uiState.collectAsState()
    val refreshVersion by notifier.version.collectAsState()

    LaunchedEffect(refreshVersion) {
        if (refreshVersion > 0) viewModel.refresh()
    }

    CharacterManagementTab(
        modifier = modifier,
        indexItems = state.indexItems,
        disabledChars = state.disabledChars,
        allProgress = state.allProgress,
        todayEpochDay = state.todayEpochDay,
        selectedChar = state.selectedChar,
        selectedItem = state.selectedItem,
        progress = state.progress,
        overridePhrases = state.overridePhrases,
        newPhrase = state.newPhrase,
        onNewPhraseChange = { viewModel.newPhraseChange(it) },
        onSelectChar = { viewModel.selectCharacter(it) },
        onToggleEnabled = { c, e -> viewModel.toggleCharacterEnabled(c, e) },
        onSavePhraseOverride = { c, p -> viewModel.savePhraseOverride(c, p) },
        onDeletePhraseOverride = { viewModel.deletePhraseOverride(it) },
        onMarkDueToday = { viewModel.markDueToday(it) },
        onResetProgress = { viewModel.resetProgress(it) },
        onResetWrongCount = { viewModel.resetWrongCount(it) },
        onBulkDisable = { viewModel.bulkDisable(it) },
        onBulkEnable = { viewModel.bulkEnable(it) },
        onBack = onBack,
    )
}

@Composable
fun LearningDataTabRoute(
    modifier: Modifier,
    onBack: () -> Unit,
    factories: AdminViewModelFactories,
    notifier: AdminDataChangedNotifier,
) {
    val factory = remember(factories) { factories.learningFactory() }
    val viewModel: AdminLearningDataViewModel = viewModel(factory = factory, key = "admin_learning")
    val state by viewModel.uiState.collectAsState()
    val refreshVersion by notifier.version.collectAsState()

    LaunchedEffect(refreshVersion) {
        if (refreshVersion > 0) viewModel.refresh()
    }

    LearningDataTab(
        modifier = modifier,
        indexItems = state.indexItems,
        allProgress = state.allProgress,
        onClearAll = { viewModel.clearAll() },
        onClearProgress = { viewModel.clearProgress() },
        onCleanupOrphanProgress = { viewModel.cleanupOrphanProgress(it) },
        onBack = onBack,
    )
}

@Composable
fun SettingsTabRoute(
    modifier: Modifier,
    onBack: () -> Unit,
    factories: AdminViewModelFactories,
    notifier: AdminDataChangedNotifier,
) {
    val factory = remember(factories) { factories.settingsFactory() }
    val viewModel: AdminSettingsViewModel = viewModel(factory = factory, key = "admin_settings")
    val state by viewModel.uiState.collectAsState()
    val refreshVersion by notifier.version.collectAsState()

    LaunchedEffect(refreshVersion) {
        if (refreshVersion > 0) viewModel.load()
    }

    SettingsTab(
        modifier = modifier,
        settings = state.settings,
        onUpdateSettings = { viewModel.updateSettings(it) },
        onBack = onBack,
    )
}

@Composable
fun BackupTabRoute(
    modifier: Modifier,
    onBack: () -> Unit,
    factories: AdminViewModelFactories,
    notifier: AdminDataChangedNotifier,
    indexItems: List<CharIndexItem>,
) {
    val factory = remember(factories, notifier) {
        factories.backupFactory(onDataChanged = notifier::notifyDataChanged)
    }
    val viewModel: AdminBackupViewModel = viewModel(factory = factory, key = "admin_backup")
    val state by viewModel.uiState.collectAsState()

    var backupImportMode by remember { mutableStateOf(ImportMode.Replace) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri != null) viewModel.export(uri, state.exportOptions)
        },
    )
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) viewModel.importData(uri, backupImportMode)
        },
    )
    val importPhrasesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) viewModel.importPhrases(uri)
        },
    )
    val importCurriculumLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.importCurriculum(uri, state.curriculumDisableOthers, indexItems)
            }
        },
    )
    val importStrokesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) viewModel.importStrokes(uri)
        },
    )

    BackupTab(
        modifier = modifier,
        importStatus = state.importStatus,
        onExport = { opts, name ->
            viewModel.updateExportOptions(opts)
            exportLauncher.launch(name)
        },
        onImport = { mode ->
            backupImportMode = mode
            importLauncher.launch(arrayOf("application/json"))
        },
        onRequestImportStrokes = {
            importStrokesLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
        },
        onRequestImportPhrases = {
            importPhrasesLauncher.launch(arrayOf("application/json"))
        },
        onRequestImportCurriculum = { disableOthers ->
            viewModel.updateCurriculumDisableOthers(disableOthers)
            importCurriculumLauncher.launch(
                arrayOf("text/plain", "application/json", "text/csv", "application/csv", "application/octet-stream", "*/*")
            )
        },
        onBack = onBack,
    )
}
