package com.hanzi.learner.feature.practice.domain

import com.hanzi.learner.db.AppSettings
import com.hanzi.learner.db.AppSettingsRepositoryContract
import com.hanzi.learner.db.DisabledCharRepositoryContract
import com.hanzi.learner.db.PhraseOverrideData
import com.hanzi.learner.db.ProgressRepositoryContract
import com.hanzi.learner.hanzi.data.CharIndexItem
import com.hanzi.learner.hanzi.data.CharacterRepository
import com.hanzi.learner.hanzi.model.CharacterData
import com.hanzi.learner.hanzi.model.Point
import com.hanzi.learner.feature.common.ports.CharacterRepositoryProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSessionOrchestratorTest {

    private fun item(char: String): CharIndexItem {
        return CharIndexItem(
            char = char,
            codepoint = char.codePointAt(0),
            file = "$char.json",
            pinyin = listOf("test"),
            strokeCount = 3,
            phrases = listOf("测试$char"),
        )
    }

    private fun characterData(char: String, strokeCount: Int = 3): CharacterData {
        return CharacterData(
            char = char,
            strokes = List(strokeCount) { "M0,0L10,10" },
            medians = List(strokeCount) { listOf(Point(0f, 0f), Point(10f, 10f)) },
        )
    }

    private class FakeAppSettingsRepository(
        private val settings: AppSettings = AppSettings(),
    ) : AppSettingsRepositoryContract {
        override suspend fun getSettings(): AppSettings = settings
        override suspend fun updateSettings(settings: AppSettings) {}
    }

    private class FakeDisabledCharRepository(
        private val disabledChars: List<String> = emptyList(),
    ) : DisabledCharRepositoryContract {
        override suspend fun getAllDisabledChars(): List<String> = disabledChars
        override suspend fun enable(char: String) {}
        override suspend fun disable(char: String) {}
    }

    private class FakePhraseProvider(
        private val overrides: Map<String, PhraseOverrideData> = emptyMap(),
    ) : PracticePhraseProvider {
        override suspend fun phrasesFor(item: CharIndexItem): List<String> {
            return overrides[item.char]?.phrases.orEmpty()
        }
    }

    private class FakeCharacterRepository(
        private val index: List<CharIndexItem>,
        private val characters: Map<String, CharacterData>,
    ) : CharacterRepository {
        override suspend fun loadIndex(): List<CharIndexItem> = index
        override suspend fun loadCharacter(item: CharIndexItem): CharacterData =
            characters[item.char] ?: throw IllegalArgumentException("Character not found: ${item.char}")
    }

    private class FakeProgressRepository(
        private val dueChars: List<String> = emptyList(),
        private val learnedChars: List<String> = emptyList(),
    ) : ProgressRepositoryContract {
        override suspend fun recordCompletion(char: String, totalMistakes: Int) {}
        override suspend fun getDueChars(limit: Int): List<String> = dueChars.take(limit)
        override suspend fun getDueCount(): Int = dueChars.size
        override suspend fun getAllLearnedChars(): List<String> = learnedChars
    }

    private fun createOrchestrator(
        index: List<CharIndexItem> = emptyList(),
        characters: Map<String, CharacterData> = emptyMap(),
        dueChars: List<String> = emptyList(),
        learnedChars: List<String> = emptyList(),
        disabledChars: List<String> = emptyList(),
        settings: AppSettings = AppSettings(),
    ): PracticeSessionOrchestrator {
        val charRepo = FakeCharacterRepository(index, characters)
        val characterRepositoryProvider = object : CharacterRepositoryProvider {
            override fun get(useExternalDataset: Boolean): CharacterRepository = charRepo
        }
        val progressRepository = FakeProgressRepository(dueChars, learnedChars)
        val pickNextItem = PickNextPracticeItemUseCase(progressRepository)

        return PracticeSessionOrchestrator(
            appSettingsRepository = FakeAppSettingsRepository(settings),
            disabledCharRepository = FakeDisabledCharRepository(disabledChars),
            characterRepositoryProvider = characterRepositoryProvider,
            itemSelector = pickNextItem,
            phraseProvider = FakePhraseProvider(),
        )
    }

    @Test
    fun startSession_withEmptyIndex_returnsSessionComplete() = runTest {
        val orchestrator = createOrchestrator(
            index = emptyList(),
            characters = emptyMap(),
        )

        val session = orchestrator.create(reviewOnly = false)
        val state = session.startSession()

        assertTrue(state.isSessionComplete)
        assertNull(state.currentCharacter)
        assertNull(state.currentItem)
        assertEquals(emptyList<CharIndexItem>(), state.windowItems)
    }

    @Test
    fun startSession_withAllCharsDisabled_returnsAllDisabled() = runTest {
        val orchestrator = createOrchestrator(
            index = listOf(item("一"), item("二")),
            characters = mapOf(
                "一" to characterData("一"),
                "二" to characterData("二"),
            ),
            disabledChars = listOf("一", "二"),
        )

        val session = orchestrator.create(reviewOnly = false)
        val state = session.startSession()

        assertTrue(state.isSessionComplete)
        assertTrue(state.allDisabled)
        assertEquals(emptyList<CharIndexItem>(), state.windowItems)
    }

    @Test
    fun startSession_withValidData_returnsCharacterDataAndWindow() = runTest {
        val orchestrator = createOrchestrator(
            index = listOf(item("一"), item("二"), item("三")),
            characters = mapOf(
                "一" to characterData("一"),
                "二" to characterData("二"),
                "三" to characterData("三"),
            ),
            dueChars = listOf("一", "二", "三"),
        )

        val session = orchestrator.create(reviewOnly = false)
        val state = session.startSession()

        assertEquals(false, state.isSessionComplete)
        assertNotNull(state.currentCharacter)
        assertNotNull(state.currentItem)
        assertEquals("一", state.currentItem?.char)
        assertEquals("一", state.currentCharacter?.char)
        assertEquals(0, state.strokeIndex)
        assertEquals(0, state.completedStrokeCount)
        assertEquals(0, state.mistakesOnStroke)
        val defaultWindowSize = 5
        assertTrue(state.windowItems.size <= defaultWindowSize)
    }

    @Test
    fun startSession_reviewOnlyWithNoDueChars_returnsNoReviewsDue() = runTest {
        val orchestrator = createOrchestrator(
            index = listOf(item("一"), item("二")),
            characters = mapOf(
                "一" to characterData("一"),
                "二" to characterData("二"),
            ),
            dueChars = emptyList(),
            learnedChars = listOf("一", "二"),
        )

        val session = orchestrator.create(reviewOnly = true)
        val state = session.startSession()

        assertTrue(state.isSessionComplete)
        assertTrue(state.noReviewsDue)
        assertEquals(emptyList<CharIndexItem>(), state.windowItems)
    }

    @Test
    fun processCharCompletion_returnsCompletionResultWithCorrectState() = runTest {
        val orchestrator = createOrchestrator(
            index = listOf(item("一"), item("二")),
            characters = mapOf(
                "一" to characterData("一"),
                "二" to characterData("二"),
            ),
            dueChars = listOf("一", "二"),
        )

        val session = orchestrator.create(reviewOnly = false)
        session.startSession()
        val result = session.processCharCompletion(finishedChar = "一")

        assertNotNull(result)
        assertNotNull(result.state)
        assertEquals(false, result.state.isSessionComplete)
        val movedToNextCharacter = result.state.currentCharacter != null
        assertTrue(movedToNextCharacter)
    }

    @Test
    fun processCharCompletion_withEmptyWindow_returnsSessionComplete() = runTest {
        val orchestrator = createOrchestrator(
            index = listOf(item("一")),
            characters = mapOf("一" to characterData("一")),
            dueChars = listOf("一"),
        )

        val session = orchestrator.create(reviewOnly = true)
        session.startSession()
        
        val defaultRequiredReps = 2
        repeat(defaultRequiredReps) {
            session.processCharCompletion(finishedChar = "一")
        }
        val result = session.processCharCompletion(finishedChar = "一")

        assertTrue(result.state.isSessionComplete)
        assertNull(result.state.currentCharacter)
    }

    @Test
    fun recordMistake_delegatesToSessionManager() = runTest {
        val orchestrator = createOrchestrator(
            index = listOf(item("一")),
            characters = mapOf("一" to characterData("一")),
            dueChars = listOf("一"),
        )

        val session = orchestrator.create(reviewOnly = false)
        session.startSession()
        
        session.recordMistake("一")
        
        val result = session.processCharCompletion(finishedChar = "一")
        
        val firstCompletionDoesNotReturnRecordedCompletion = result.completion == null
        assertTrue(firstCompletionDoesNotReturnRecordedCompletion)
    }
}
