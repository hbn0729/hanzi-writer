package com.hanzi.learner.features.practice.domain

import com.hanzi.learner.data.model.AppSettings
import com.hanzi.learner.data.repository.AppSettingsRepositoryContract
import com.hanzi.learner.data.repository.DisabledCharRepositoryContract
import com.hanzi.learner.character-writer.data.CharIndexItem
import com.hanzi.learner.character-writer.data.CharacterRepository
import com.hanzi.learner.character-writer.model.CharacterData
import com.hanzi.learner.features.common.ports.CharacterRepositoryProvider

class PracticeSessionOrchestrator(
    private val appSettingsRepository: AppSettingsRepositoryContract,
    private val disabledCharRepository: DisabledCharRepositoryContract,
    private val characterRepositoryProvider: CharacterRepositoryProvider,
    private val itemSelector: PracticeItemSelector,
    private val phraseProvider: PracticePhraseProvider,
    private val windowManagerFactory: () -> PracticeWindowManager = { PracticeSessionManager() },
) : PracticeSessionEngineFactory {
    override fun create(reviewOnly: Boolean): PracticeSessionEngine {
        return Session(reviewOnly)
    }

    private inner class Session(
        private val reviewOnly: Boolean,
    ) : PracticeSessionEngine {
        private val sessionManager = windowManagerFactory()
        private val stateLoader = CurrentCharacterLoader(phraseProvider)
        private var index: List<CharIndexItem>? = null
        private var disabledChars = setOf<String>()
        private var settings = AppSettings()
        private var charRepo: CharacterRepository? = null

        override fun recordMistake(char: String) {
            sessionManager.recordMistake(char)
        }

        override suspend fun startSession(): PracticeSessionState {
            val loadedSettings = appSettingsRepository.getSettings()
            settings = loadedSettings
            charRepo = characterRepositoryProvider.get(loadedSettings.useExternalDataset)

            val repo = charRepo ?: return PracticeSessionState(isSessionComplete = true)

            val loadedIndex = repo.loadIndex()
            index = loadedIndex
            if (loadedIndex.isEmpty()) {
                return PracticeSessionState(isSessionComplete = true)
            }

            disabledChars = disabledCharRepository.getAllDisabledChars().toHashSet()
            sessionManager.reset()

            val exclude = LinkedHashSet<String>()
            val initialItems = mutableListOf<CharIndexItem>()
            while (initialItems.size < sessionManager.windowSize) {
                val next = pickNext(
                    index = loadedIndex,
                    disabledChars = disabledChars,
                    excludeChars = exclude,
                ) ?: break
                initialItems.add(next)
                exclude.add(next.char)
            }
            sessionManager.start(initialItems)

            if (sessionManager.windowItems.isEmpty()) {
                val availableCount = loadedIndex.count { it.char !in disabledChars }
                return PracticeSessionState(
                    isSessionComplete = true,
                    allDisabled = availableCount == 0,
                    noReviewsDue = availableCount > 0,
                )
            }

            return loadCurrentChar()
        }

        override suspend fun processCharCompletion(finishedChar: String): PracticeCompletionResult {
            disabledChars = disabledCharRepository.getAllDisabledChars().toHashSet()
            sessionManager.removeDisabled(disabledChars)

            if (sessionManager.windowItems.isEmpty()) {
                return PracticeCompletionResult(
                    completion = null,
                    state = PracticeSessionState(isSessionComplete = true),
                )
            }

            val idx = index
            val completion = sessionManager.onCharCompleted(
                finishedChar = finishedChar,
                pickNext = { excludeChars ->
                    if (idx == null) return@onCharCompleted null
                    pickNext(
                        index = idx,
                        disabledChars = disabledChars,
                        excludeChars = excludeChars,
                    )
                },
            )

            return PracticeCompletionResult(
                completion = completion,
                state = loadCurrentChar(),
            )
        }

        private suspend fun loadCurrentChar(): PracticeSessionState {
            return stateLoader.load(
                repo = charRepo,
                windowManager = sessionManager,
                hintAfterMisses = settings.hintAfterMisses,
            )
        }

        private suspend fun pickNext(
            index: List<CharIndexItem>,
            disabledChars: Set<String>,
            excludeChars: Set<String>,
        ): CharIndexItem? {
            return itemSelector.pickNext(
                PracticeSelectionRequest(
                    index = index,
                    disabledChars = disabledChars,
                    excludeChars = excludeChars,
                    limit = settings.duePickLimit,
                    strategy = if (reviewOnly) {
                        PracticeItemSelectionStrategy.ReviewOnly
                    } else {
                        PracticeItemSelectionStrategy.NewThenDue
                    },
                )
            )
        }
    }
}
