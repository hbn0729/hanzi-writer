package com.hanzi.learner.feature.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hanzi.learner.db.TimeProvider
import com.hanzi.learner.feature.admin.domain.LoadAdminDashboardUseCase
import com.hanzi.learner.feature.admin.model.AdminProgress
import com.hanzi.learner.feature.admin.model.AdminStudyCount
import com.hanzi.learner.feature.admin.repository.AdminProgressCommandRepository
import com.hanzi.learner.hanzi.data.CharIndexItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminDashboardData(
    val totalChars: Int,
    val enabledCount: Int,
    val disabledCount: Int,
    val learnedCount: Int,
    val unlearnedCount: Int,
    val dueCount: Int,
    val phraseOverrideCount: Int,
    val topWrong: List<AdminProgress>,
    val dueProgress: List<AdminProgress>,
    val studyCounts: List<AdminStudyCount>,
)

data class AdminDashboardUiState(
    val isLoading: Boolean = false,
    val data: AdminDashboardData? = null,
    val error: String? = null,
    val indexItems: List<CharIndexItem> = emptyList(),
    val disabledChars: Set<String> = emptySet(),
)

class AdminDashboardViewModel(
    private val progressCommandRepository: AdminProgressCommandRepository,
    private val timeProvider: TimeProvider,
    private val loadDashboardUseCase: LoadAdminDashboardUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val snapshot = loadDashboardUseCase()

                val data = AdminDashboardData(
                    totalChars = snapshot.totalChars,
                    enabledCount = snapshot.enabledCount,
                    disabledCount = snapshot.disabledCount,
                    learnedCount = snapshot.learnedCount,
                    unlearnedCount = snapshot.unlearnedCount,
                    dueCount = snapshot.dueCount,
                    phraseOverrideCount = snapshot.phraseOverrideCount,
                    topWrong = snapshot.topWrong,
                    dueProgress = snapshot.dueProgress,
                    studyCounts = snapshot.studyCounts,
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        data = data,
                        indexItems = snapshot.indexItems,
                        disabledChars = snapshot.disabledChars,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun markDueToday(chars: List<String>) {
        viewModelScope.launch {
            progressCommandRepository.updateNextDueDay(chars, timeProvider.todayEpochDay())
            loadDashboard()
        }
    }

    fun resetProgress(chars: List<String>) {
        viewModelScope.launch {
            progressCommandRepository.deleteProgressByChars(chars)
            loadDashboard()
        }
    }

    class Factory(
        private val progressCommandRepository: AdminProgressCommandRepository,
        private val timeProvider: TimeProvider,
        private val loadDashboardUseCase: LoadAdminDashboardUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminDashboardViewModel::class.java)) {
                return AdminDashboardViewModel(
                    progressCommandRepository = progressCommandRepository,
                    timeProvider = timeProvider,
                    loadDashboardUseCase = loadDashboardUseCase,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
