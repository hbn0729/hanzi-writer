package com.hanzi.learner.features.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hanzi.learner.features.practice.domain.CompletePracticeCharacterUseCase
import com.hanzi.learner.features.practice.domain.PracticeSessionEngine
import com.hanzi.learner.features.practice.domain.PracticeSessionEngineFactory
import com.hanzi.learner.features.practice.domain.PracticeSessionState
import com.hanzi.learner.character_writer.data.CharIndexItem
import com.hanzi.learner.character_writer.model.CharacterData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class StrokeUpdate(
    val flashState: FlashState,
    val strokeIndex: Int,
    val completedStrokeCount: Int,
    val mistakesOnStroke: Int,
    val shouldCompleteCharacter: Boolean = false,
)

private class PracticeStrokeHandler {
    fun onMatch(state: PracticeUiState): StrokeUpdate {
        val strokeCount = state.currentCharacter?.medians?.size ?: 0
        if (strokeCount <= 0) {
            return StrokeUpdate(
                flashState = FlashState.Success,
                strokeIndex = state.strokeIndex,
                completedStrokeCount = state.completedStrokeCount,
                mistakesOnStroke = 0,
            )
        }

        val nextStroke = state.strokeIndex + 1
        if (nextStroke >= strokeCount) {
            return StrokeUpdate(
                flashState = FlashState.Success,
                strokeIndex = state.strokeIndex,
                completedStrokeCount = nextStroke,
                mistakesOnStroke = 0,
                shouldCompleteCharacter = true,
            )
        }

        return StrokeUpdate(
            flashState = FlashState.Success,
            strokeIndex = nextStroke,
            completedStrokeCount = nextStroke,
            mistakesOnStroke = 0,
        )
    }

    fun onMiss(state: PracticeUiState): StrokeUpdate {
        return StrokeUpdate(
            flashState = FlashState.Failure,
            strokeIndex = state.strokeIndex,
            completedStrokeCount = state.completedStrokeCount,
            mistakesOnStroke = state.mistakesOnStroke + 1,
        )
    }
}

private class PracticeEngineCoordinator(
    private val reviewOnly: Boolean,
    private val engineFactory: PracticeSessionEngineFactory,
    private val completePracticeCharacter: CompletePracticeCharacterUseCase,
) {
    private var engine: PracticeSessionEngine? = null

    suspend fun startSession(): PracticeSessionState {
        val activeEngine = engineFactory.create(reviewOnly)
        engine = activeEngine
        return activeEngine.startSession()
    }

    fun recordMistake(char: String) {
        engine?.recordMistake(char)
    }

    suspend fun completeCharacter(finishedChar: String): PracticeSessionState? {
        val activeEngine = engine ?: return null
        return completePracticeCharacter(activeEngine, finishedChar)
    }
}

data class PracticeUiState(
    val isLoading: Boolean = false,
    val isSessionComplete: Boolean = false,
    val allDisabled: Boolean = false,
    val noReviewsDue: Boolean = false,
    val currentCharacter: CharacterData? = null,
    val currentItem: CharIndexItem? = null,
    val currentPhrase: String = "",
    val strokeIndex: Int = 0,
    val completedStrokeCount: Int = 0,
    val mistakesOnStroke: Int = 0,
    val hintAfterMisses: Int = 2,
    val flashColorState: FlashState = FlashState.None,
    val windowItems: List<CharIndexItem> = emptyList(),
)

enum class FlashState {
    None, Success, Failure
}

sealed interface PracticeAction {
    object Start : PracticeAction
    data class StrokeResult(val isMatch: Boolean) : PracticeAction
    object ClearFlash : PracticeAction
}

class PracticeViewModel(
    private val reviewOnly: Boolean,
    private val engineFactory: PracticeSessionEngineFactory,
    private val completePracticeCharacter: CompletePracticeCharacterUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()
    private val strokeHandler = PracticeStrokeHandler()
    private val engineCoordinator = PracticeEngineCoordinator(
        reviewOnly = reviewOnly,
        engineFactory = engineFactory,
        completePracticeCharacter = completePracticeCharacter,
    )

    fun onAction(action: PracticeAction) {
        when (action) {
            is PracticeAction.Start -> startSession()
            is PracticeAction.StrokeResult -> handleStroke(action.isMatch)
            is PracticeAction.ClearFlash -> {
                _uiState.update {
                    it.copy(flashColorState = FlashState.None)
                }
            }
        }
    }

    private fun startSession() {
        viewModelScope.launch {
            applyState(engineCoordinator.startSession())
        }
    }

    private fun handleStroke(isMatch: Boolean) {
        val state = _uiState.value
        val update = if (isMatch) {
            strokeHandler.onMatch(state)
        } else {
            state.currentItem?.char?.let { engineCoordinator.recordMistake(it) }
            strokeHandler.onMiss(state)
        }

        _uiState.update {
            it.copy(
                flashColorState = update.flashState,
                strokeIndex = update.strokeIndex,
                completedStrokeCount = update.completedStrokeCount,
                mistakesOnStroke = update.mistakesOnStroke,
            )
        }

        if (update.shouldCompleteCharacter) {
            viewModelScope.launch {
                processCharCompletion()
            }
        }
    }

    private suspend fun processCharCompletion() {
        val finishedChar = _uiState.value.currentItem?.char ?: return
        val nextState = engineCoordinator.completeCharacter(finishedChar) ?: return
        applyState(nextState)
    }

    private fun applyState(state: PracticeSessionState) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isSessionComplete = state.isSessionComplete,
                allDisabled = state.allDisabled,
                noReviewsDue = state.noReviewsDue,
                currentCharacter = state.currentCharacter,
                currentItem = state.currentItem,
                currentPhrase = state.currentPhrase,
                strokeIndex = state.strokeIndex,
                completedStrokeCount = state.completedStrokeCount,
                mistakesOnStroke = state.mistakesOnStroke,
                hintAfterMisses = state.hintAfterMisses,
                windowItems = state.windowItems,
            )
        }
    }

    class Factory(
        private val reviewOnly: Boolean,
        private val engineFactory: PracticeSessionEngineFactory,
        private val completePracticeCharacter: CompletePracticeCharacterUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (!modelClass.isAssignableFrom(PracticeViewModel::class.java)) {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
            return PracticeViewModel(
                reviewOnly = reviewOnly,
                engineFactory = engineFactory,
                completePracticeCharacter = completePracticeCharacter,
            ) as T
        }
    }
}
