package com.hanzi.learner.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hanzi.learner.feature.home.domain.LoadHomeDataUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PracticeMode {
    PRACTICE, REVIEW
}

data class HomeData(
    val unlearnedCount: Int,
    val reviewCount: Int,
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val data: HomeData? = null,
    val error: String? = null,
)

sealed interface HomeAction {
    object LoadData : HomeAction
    data class SelectMode(val mode: PracticeMode) : HomeAction
}

sealed interface HomeNavigation {
    object NavigateToPractice : HomeNavigation
    object NavigateToReview : HomeNavigation
}

class HomeViewModel(
    private val navigationCallback: (HomeNavigation) -> Unit = {},
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val loadHomeDataUseCase: LoadHomeDataUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.LoadData -> loadData()
            is HomeAction.SelectMode -> selectMode(action.mode)
        }
    }

    private fun loadData() {
        viewModelScope.launch(dispatcher) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val counts = loadHomeDataUseCase()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        data = HomeData(counts.unlearnedCount, counts.reviewCount),
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    private fun selectMode(mode: PracticeMode) {
        navigationCallback(
            when (mode) {
                PracticeMode.PRACTICE -> HomeNavigation.NavigateToPractice
                PracticeMode.REVIEW -> HomeNavigation.NavigateToReview
            }
        )
    }

    class Factory(
        private val loadHomeDataUseCase: LoadHomeDataUseCase,
        private val navigationCallback: (HomeNavigation) -> Unit = {},
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(
                    navigationCallback = navigationCallback,
                    loadHomeDataUseCase = loadHomeDataUseCase,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
