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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class CharFilterMode(val label: String) {
    ALL("全部"),
    DUE("到期"),
    LEARNED("已学"),
    UNLEARNED("未学"),
    DISABLED("禁用"),
}

data class FilteredCharacterResult(
    val totalCount: Int = 0,
    val visibleItems: List<CharIndexItem> = emptyList(),
)

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

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class AdminCharacterViewModel(
    private val indexRepository: AdminIndexRepository,
    private val progressQueryRepository: AdminProgressQueryRepository,
    private val progressCommandRepository: AdminProgressCommandRepository,
    private val phraseOverrideRepository: AdminPhraseOverrideRepository,
    private val disabledCharRepository: AdminDisabledCharRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {
    // PWR-02: Serialize bulk I/O operations to prevent concurrent conflicts
    private val singleOp = Dispatchers.IO.limitedParallelism(1)

    private val _isOperating = MutableStateFlow(false)
    val isOperating: StateFlow<Boolean> = _isOperating.asStateFlow()

    private val _uiState = MutableStateFlow(AdminCharacterUiState())
    val uiState: StateFlow<AdminCharacterUiState> = _uiState.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _filterMode = MutableStateFlow(CharFilterMode.ALL)
    val filterMode: StateFlow<CharFilterMode> = _filterMode.asStateFlow()

    private val _displayCount = MutableStateFlow(PAGE_SIZE)

    private data class FilterParams(
        val searchText: String,
        val filterMode: CharFilterMode,
        val displayCount: Int,
        val state: AdminCharacterUiState,
    )

    // PWR-03: Debounce only search text to avoid delaying "load more" / "refresh"
    @Suppress("OPT_IN_USAGE")
    private val debouncedSearchText = _searchText.debounce(150).distinctUntilChanged()

    val filteredResult: StateFlow<FilteredCharacterResult> = combine(
        debouncedSearchText, _filterMode, _displayCount, _uiState
    ) { search, filter, count, state ->
        FilterParams(search, filter, count, state)
    }.mapLatest { params ->
        withContext(Dispatchers.Default) {
            val visible = ArrayList<CharIndexItem>(minOf(params.displayCount, 200))
            var total = 0
            for (item in params.state.indexItems) {
                if (!matchesFilter(item, params.searchText, params.filterMode, params.state)) continue
                total++
                if (visible.size < params.displayCount) {
                    visible.add(item)
                }
            }
            FilteredCharacterResult(totalCount = total, visibleItems = visible)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FilteredCharacterResult())

    init {
        refresh()
    }

    fun updateSearchText(text: String) {
        _searchText.value = text
        _displayCount.value = PAGE_SIZE
    }

    fun updateFilterMode(mode: CharFilterMode) {
        _filterMode.value = mode
        _displayCount.value = PAGE_SIZE
    }

    fun loadMore() {
        _displayCount.value += PAGE_SIZE
    }

    fun refresh() {
        viewModelScope.launch {
            refreshInternal()
        }
    }

    private suspend fun refreshInternal() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val indexItems = indexRepository.loadIndex()
            val disabledChars = disabledCharRepository.getDisabledChars()
            val allProgress = progressQueryRepository.getAllProgress()
            val selectedChar = _uiState.value.selectedChar

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

            if (selectedChar != null) selectCharacterInternal(selectedChar)
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
        }
    }

    fun selectCharacter(char: String?) {
        viewModelScope.launch {
            selectCharacterInternal(char)
        }
    }

    private suspend fun selectCharacterInternal(char: String?) {
        if (char == null) {
            _uiState.update {
                it.copy(
                    selectedChar = null,
                    selectedItem = null,
                    progress = null,
                    overridePhrases = emptyList(),
                )
            }
            return
        }
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
        viewModelScope.launch(singleOp) {
            _isOperating.value = true
            try {
                disabledCharRepository.disableAll(chars)
                refreshInternal()
            } finally {
                _isOperating.value = false
            }
        }
    }

    fun bulkEnable(chars: List<String>) {
        viewModelScope.launch(singleOp) {
            _isOperating.value = true
            try {
                disabledCharRepository.enableAll(chars)
                refreshInternal()
            } finally {
                _isOperating.value = false
            }
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

    companion object {
        private const val PAGE_SIZE = 20

        private fun matchesFilter(
            item: CharIndexItem,
            searchText: String,
            filterMode: CharFilterMode,
            state: AdminCharacterUiState,
        ): Boolean {
            val ch = item.char
            val p = state.allProgress[ch]
            val isDisabled = ch in state.disabledChars
            val isLearned = p != null
            val isDue = p != null && p.nextDueDay <= state.todayEpochDay
            val searchOk = searchText.isBlank() ||
                ch.contains(searchText) ||
                item.pinyin.any { it.contains(searchText, ignoreCase = true) } ||
                item.strokeCount.toString() == searchText

            val filterOk = when (filterMode) {
                CharFilterMode.ALL -> true
                CharFilterMode.DUE -> isDue
                CharFilterMode.LEARNED -> isLearned
                CharFilterMode.UNLEARNED -> !isLearned
                CharFilterMode.DISABLED -> isDisabled
            }

            return searchOk && filterOk
        }
    }
}
