package com.hanzi.learner.features.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hanzi.learner.data.model.TimeProvider
import com.hanzi.learner.features.admin.model.AdminPhraseOverride
import com.hanzi.learner.features.admin.model.AdminProgress
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

data class AdminCharacterUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val indexItems: List<CharIndexItem> = emptyList(),
    val disabledChars: Set<String> = emptySet(),
    val allProgress: Map<String, AdminProgress> = emptyMap(),
    val selectedChar: String? = null,
    val selectedItem: CharIndexItem? = null,
    val progress: AdminProgress? = null,
    val overridePhrases: List<String> = emptyList(),
    val newPhrase: String = "",
    val todayEpochDay: Long = 0L,
)

class AdminCharacterViewModel(
    private val indexRepository: AdminIndexRepository,
    private val progressQueryRepository: AdminProgressQueryRepository,
    private val progressCommandRepository: AdminProgressCommandRepository,
    private val phraseOverrideRepository: AdminPhraseOverrideRepository,
    private val disabledCharRepository: AdminDisabledCharRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdminCharacterUiState())
    val uiState: StateFlow<AdminCharacterUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val indexItems = indexRepository.loadIndex()
                val disabledChars = disabledCharRepository.getDisabledChars()
                val allProgress = progressQueryRepository.getAllProgress()
                val selectedChar = _uiState.value.selectedChar ?: indexItems.firstOrNull()?.char

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        indexItems = indexItems,
                        disabledChars = disabledChars,
                        allProgress = allProgress,
                        selectedChar = selectedChar,
                        todayEpochDay = timeProvider.todayEpochDay(),
                    )
                }

                if (selectedChar != null) selectCharacter(selectedChar)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun selectCharacter(char: String) {
        viewModelScope.launch {
            val item = _uiState.value.indexItems.firstOrNull { it.char == char }
            val progress = progressQueryRepository.getProgress(char)
            val po = phraseOverrideRepository.getPhraseOverride(char)
            val phrases = po?.phrases.orEmpty()
            _uiState.update {
                it.copy(
                    selectedChar = char,
                    selectedItem = item,
                    progress = progress,
                    overridePhrases = phrases,
                )
            }
        }
    }

    fun newPhraseChange(text: String) {
        _uiState.update { it.copy(newPhrase = text) }
    }

    fun toggleCharacterEnabled(char: String, enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) disabledCharRepository.enableCharacter(char) else disabledCharRepository.disableCharacter(char)
            refresh()
        }
    }

    fun savePhraseOverride(char: String, phrases: List<String>) {
        viewModelScope.launch {
            phraseOverrideRepository.savePhraseOverride(AdminPhraseOverride(char = char, phrases = phrases))
            _uiState.update { it.copy(overridePhrases = phrases, newPhrase = "") }
            refresh()
        }
    }

    fun deletePhraseOverride(char: String) {
        viewModelScope.launch {
            phraseOverrideRepository.deletePhraseOverride(char)
            _uiState.update { it.copy(overridePhrases = emptyList()) }
            refresh()
        }
    }

    fun markDueToday(chars: List<String>) {
        viewModelScope.launch {
            progressCommandRepository.updateNextDueDay(chars, timeProvider.todayEpochDay())
            refresh()
        }
    }

    fun resetProgress(chars: List<String>) {
        viewModelScope.launch {
            progressCommandRepository.deleteProgressByChars(chars)
            refresh()
        }
    }

    fun resetWrongCount(chars: List<String>) {
        viewModelScope.launch {
            progressCommandRepository.resetWrongCount(chars)
            refresh()
        }
    }

    fun bulkDisable(chars: List<String>) {
        viewModelScope.launch {
            disabledCharRepository.disableAll(chars)
            refresh()
        }
    }

    fun bulkEnable(chars: List<String>) {
        viewModelScope.launch {
            disabledCharRepository.enableAll(chars)
            refresh()
        }
    }

    class Factory(
        private val indexRepository: AdminIndexRepository,
        private val progressQueryRepository: AdminProgressQueryRepository,
        private val progressCommandRepository: AdminProgressCommandRepository,
        private val phraseOverrideRepository: AdminPhraseOverrideRepository,
        private val disabledCharRepository: AdminDisabledCharRepository,
        private val timeProvider: TimeProvider,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminCharacterViewModel::class.java)) {
                return AdminCharacterViewModel(
                    indexRepository = indexRepository,
                    progressQueryRepository = progressQueryRepository,
                    progressCommandRepository = progressCommandRepository,
                    phraseOverrideRepository = phraseOverrideRepository,
                    disabledCharRepository = disabledCharRepository,
                    timeProvider = timeProvider,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
