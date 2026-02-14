package com.hanzi.learner.feature.practice.domain

import com.hanzi.learner.hanzi.data.CharIndexItem
import com.hanzi.learner.hanzi.data.CharacterRepository


class CurrentCharacterLoader(
    private val phraseProvider: PracticePhraseProvider,
) {
    suspend fun load(
        repo: CharacterRepository?,
        windowManager: PracticeWindowManager,
        hintAfterMisses: Int,
    ): PracticeSessionState {
        val activeRepo = repo ?: return PracticeSessionState(isSessionComplete = true)

        var attempts = 0
        while (attempts < windowManager.windowSize && windowManager.windowItems.isNotEmpty()) {
            val item = windowManager.currentItem() ?: break
            val loaded = runCatching { activeRepo.loadCharacter(item) }.getOrNull()
            if (loaded == null) {
                windowManager.removeAtCursor()
                attempts++
                continue
            }

            val phrase = phraseProvider.phrasesFor(item)
                .ifEmpty { item.phrases }
                .firstOrNull()
                .orEmpty()
            return PracticeSessionState(
                currentCharacter = loaded,
                currentItem = item,
                currentPhrase = phrase,
                hintAfterMisses = hintAfterMisses,
                windowItems = windowManager.windowItems,
            )
        }

        return PracticeSessionState(isSessionComplete = true)
    }
}
