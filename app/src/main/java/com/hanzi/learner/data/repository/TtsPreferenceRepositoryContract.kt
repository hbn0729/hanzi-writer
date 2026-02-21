package com.hanzi.learner.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * Contract for TTS preference repository.
 * Follows Dependency Inversion Principle - UI layer depends on this abstraction.
 */
interface TtsPreferenceRepositoryContract {
    /**
     * Returns a Flow of the currently selected model ID, or null if no model is selected.
     */
    fun getSelectedModelId(): Flow<String?>

    /**
     * Sets the selected model ID. Pass null to clear the selection.
     */
    suspend fun setSelectedModelId(modelId: String?)

    /**
     * Clears the current selection (equivalent to setSelectedModelId(null)).
     */
    suspend fun clearSelection()
}