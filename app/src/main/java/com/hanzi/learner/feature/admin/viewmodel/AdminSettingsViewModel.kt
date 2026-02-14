package com.hanzi.learner.feature.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hanzi.learner.feature.admin.model.AdminSettings
import com.hanzi.learner.feature.admin.repository.AdminAppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminSettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val settings: AdminSettings? = null,
)

class AdminSettingsViewModel(
    private val repository: AdminAppSettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminSettingsUiState())
    val uiState: StateFlow<AdminSettingsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val settings = repository.getSettings()
                _uiState.update { it.copy(isLoading = false, settings = settings) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun updateSettings(settings: AdminSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
            _uiState.update { it.copy(settings = settings) }
        }
    }

    class Factory(
        private val repository: AdminAppSettingsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminSettingsViewModel::class.java)) {
                return AdminSettingsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
