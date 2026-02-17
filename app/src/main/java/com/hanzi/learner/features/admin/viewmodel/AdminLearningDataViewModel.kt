package com.hanzi.learner.features.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hanzi.learner.features.admin.model.AdminProgress
import com.hanzi.learner.features.admin.model.AdminSettings
import com.hanzi.learner.features.admin.repository.AdminAppSettingsRepository
import com.hanzi.learner.features.admin.repository.AdminDisabledCharRepository
import com.hanzi.learner.features.admin.repository.AdminIndexRepository
import com.hanzi.learner.features.admin.repository.AdminPhraseOverrideRepository
import com.hanzi.learner.features.admin.repository.AdminProgressCommandRepository
import com.hanzi.learner.features.admin.repository.AdminProgressQueryRepository
import com.hanzi.learner.character_writer.data.CharIndexItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminLearningDataUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val indexItems: List<CharIndexItem> = emptyList(),
    val allProgress: Map<String, AdminProgress> = emptyMap(),
)

class AdminLearningDataViewModel(
    private val indexRepository: AdminIndexRepository,
    private val progressQueryRepository: AdminProgressQueryRepository,
    private val progressCommandRepository: AdminProgressCommandRepository,
    private val phraseOverrideRepository: AdminPhraseOverrideRepository,
    private val appSettingsRepository: AdminAppSettingsRepository,
    private val disabledCharRepository: AdminDisabledCharRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminLearningDataUiState())
    val uiState: StateFlow<AdminLearningDataUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshData()
        }
    }

    fun clearAll() {
        runAndRefresh {
            progressCommandRepository.deleteAllProgress()
            phraseOverrideRepository.deleteAllPhraseOverrides()
            disabledCharRepository.deleteAllDisabledChars()
        }
    }

    fun clearProgress() {
        runAndRefresh {
            progressCommandRepository.deleteAllProgress()
        }
    }

    fun clearPhraseOverrides() {
        runAndRefresh {
            phraseOverrideRepository.deleteAllPhraseOverrides()
        }
    }

    fun clearDisabledChars() {
        runAndRefresh {
            disabledCharRepository.deleteAllDisabledChars()
        }
    }

    fun resetSettings() {
        runAndRefresh {
            appSettingsRepository.updateSettings(AdminSettings())
        }
    }

    fun cleanupOrphanProgress(orphanChars: List<String>) {
        runAndRefresh {
            progressCommandRepository.deleteProgressByChars(orphanChars)
        }
    }

    private fun runAndRefresh(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                refreshData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    private suspend fun refreshData() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val indexItems = indexRepository.loadIndex()
            val allProgress = progressQueryRepository.getAllProgress()
            _uiState.update { it.copy(isLoading = false, indexItems = indexItems, allProgress = allProgress) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
        }
    }

    class Factory(
        private val indexRepository: AdminIndexRepository,
        private val progressQueryRepository: AdminProgressQueryRepository,
        private val progressCommandRepository: AdminProgressCommandRepository,
        private val phraseOverrideRepository: AdminPhraseOverrideRepository,
        private val appSettingsRepository: AdminAppSettingsRepository,
        private val disabledCharRepository: AdminDisabledCharRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminLearningDataViewModel::class.java)) {
                return AdminLearningDataViewModel(
                    indexRepository = indexRepository,
                    progressQueryRepository = progressQueryRepository,
                    progressCommandRepository = progressCommandRepository,
                    phraseOverrideRepository = phraseOverrideRepository,
                    appSettingsRepository = appSettingsRepository,
                    disabledCharRepository = disabledCharRepository,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
