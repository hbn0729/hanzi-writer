package com.hanzi.learner.features.admin.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hanzi.learner.data.model.ExportOptions
import com.hanzi.learner.data.model.ImportMode
import com.hanzi.learner.features.admin.repository.BackupDataTransferPort
import com.hanzi.learner.features.admin.repository.CurriculumImportPort
import com.hanzi.learner.features.admin.repository.PhraseImportPort
import com.hanzi.learner.features.admin.repository.StrokeImportPort
import com.hanzi.learner.character-writer.data.CharIndexItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminBackupUiState(
    val importStatus: String? = null,
    val exportOptions: ExportOptions = ExportOptions(),
    val curriculumDisableOthers: Boolean = true,
)

class AdminBackupViewModel(
    private val dataTransferPort: BackupDataTransferPort,
    private val phraseImportPort: PhraseImportPort,
    private val curriculumImportPort: CurriculumImportPort,
    private val strokeImportPort: StrokeImportPort,
    private val onDataChanged: () -> Unit = {},
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminBackupUiState())
    val uiState: StateFlow<AdminBackupUiState> = _uiState.asStateFlow()

    fun setImportStatus(status: String?) {
        _uiState.update { it.copy(importStatus = status) }
    }

    fun updateExportOptions(options: ExportOptions) {
        _uiState.update { it.copy(exportOptions = options) }
    }

    fun updateCurriculumDisableOthers(disableOthers: Boolean) {
        _uiState.update { it.copy(curriculumDisableOthers = disableOthers) }
    }

    fun export(uri: Uri, options: ExportOptions) {
        viewModelScope.launch {
            try {
                dataTransferPort.exportData(uri, options)
            } catch (e: Exception) {
                setImportStatus("导出失败: ${e.message}")
            }
        }
    }

    fun importData(uri: Uri, mode: ImportMode) {
        viewModelScope.launch {
            try {
                dataTransferPort.importData(uri, mode)
                setImportStatus(if (mode == ImportMode.Merge) "备份合并导入完成" else "备份替换导入完成")
                onDataChanged()
            } catch (e: Exception) {
                setImportStatus("导入失败: ${e.message}")
            }
        }
    }

    fun importPhrases(uri: Uri) {
        viewModelScope.launch {
            try {
                val result = phraseImportPort.importPhrases(uri)
                setImportStatus("短语库导入完成：${result.importedChars}字，${result.importedPhrases}条")
                onDataChanged()
            } catch (e: Exception) {
                setImportStatus("短语库导入失败：${e.message}")
            }
        }
    }

    fun importCurriculum(uri: Uri, disableOthers: Boolean, indexItems: List<CharIndexItem>) {
        viewModelScope.launch {
            try {
                val result = curriculumImportPort.importCurriculum(uri, disableOthers, indexItems)
                setImportStatus("课程字表导入完成：匹配 ${result.matched}，忽略 ${result.ignored}")
                onDataChanged()
            } catch (e: Exception) {
                setImportStatus("课程字表导入失败：${e.message}")
            }
        }
    }

    fun importStrokes(uri: Uri) {
        viewModelScope.launch {
            setImportStatus("笔画数据导入中...")
            try {
                val result = strokeImportPort.importStrokes(uri)
                if (result.switchedToExternalDataset) {
                    setImportStatus("笔画数据导入完成：生成 ${result.generatedChars} 个字，已切换为外部字库")
                    onDataChanged()
                } else {
                    setImportStatus("笔画数据导入失败")
                }
            } catch (e: Exception) {
                setImportStatus("笔画数据导入失败: ${e.message}")
            }
        }
    }

    class Factory(
        private val dataTransferPort: BackupDataTransferPort,
        private val phraseImportPort: PhraseImportPort,
        private val curriculumImportPort: CurriculumImportPort,
        private val strokeImportPort: StrokeImportPort,
        private val onDataChanged: () -> Unit = {},
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminBackupViewModel::class.java)) {
                return AdminBackupViewModel(
                    dataTransferPort = dataTransferPort,
                    phraseImportPort = phraseImportPort,
                    curriculumImportPort = curriculumImportPort,
                    strokeImportPort = strokeImportPort,
                    onDataChanged = onDataChanged,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
