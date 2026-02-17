package com.hanzi.learner.features.practice.viewmodel

import app.cash.turbine.test
import com.hanzi.learner.data.model.AppSettings
import com.hanzi.learner.data.model.DefaultSpacedRepetitionPolicy
import com.hanzi.learner.data.repository.AppSettingsRepositoryContract
import com.hanzi.learner.data.repository.DisabledCharRepositoryContract
import com.hanzi.learner.data.local.dao.HanziProgressDao
import com.hanzi.learner.data.local.entity.HanziProgressEntity
import com.hanzi.learner.data.model.PhraseOverrideData
import com.hanzi.learner.data.repository.ProgressRepository
import com.hanzi.learner.data.model.SystemTimeProvider
import com.hanzi.learner.data.repository.ProgressRepositoryContract
import com.hanzi.learner.data.local.entity.StudyCountRow
import com.hanzi.learner.features.practice.domain.CompletePracticeCharacterUseCase
import com.hanzi.learner.features.practice.domain.PickNextPracticeItemUseCase
import com.hanzi.learner.features.practice.domain.PracticePhraseProvider
import com.hanzi.learner.features.practice.domain.PracticeSessionOrchestrator
import com.hanzi.learner.character_writer.data.CharIndexItem
import com.hanzi.learner.character_writer.data.CharacterRepository
import com.hanzi.learner.character_writer.model.CharacterData
import com.hanzi.learner.character_writer.model.Point
import com.hanzi.learner.features.common.ports.CharacterRepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val charRepo = FakeCharacterRepository()
    private val appSettingsRepository = FakeAppSettingsRepository()
    private val disabledCharRepository = FakeDisabledCharRepository()
    private val phraseProvider = FakePhraseProvider()
    private val characterRepositoryProvider = object : CharacterRepositoryProvider {
        override fun get(useExternalDataset: Boolean): CharacterRepository = charRepo
    }

    private fun createViewModel(
        progressRepo: ProgressRepositoryContract,
        reviewOnly: Boolean = false,
    ): PracticeViewModel {
        val pickNextItem = PickNextPracticeItemUseCase(progressRepo)
        val engineFactory = PracticeSessionOrchestrator(
            appSettingsRepository = appSettingsRepository,
            disabledCharRepository = disabledCharRepository,
            characterRepositoryProvider = characterRepositoryProvider,
            itemSelector = pickNextItem,
            phraseProvider = phraseProvider,
        )
        return PracticeViewModel(
            reviewOnly = reviewOnly,
            engineFactory = engineFactory,
            completePracticeCharacter = CompletePracticeCharacterUseCase(progressRepo),
        )
    }

    private fun createFakeDao(
        dueChars: List<String> = emptyList(),
        knownChars: List<String> = emptyList(),
    ): HanziProgressDao = object : HanziProgressDao {
        private val upsertedEntities = mutableListOf<HanziProgressEntity>()
        private fun completedChars() = upsertedEntities.map { it.char }.toSet()

        override suspend fun getByChar(hanziChar: String): HanziProgressEntity? = upsertedEntities.find { it.char == hanziChar }
        override suspend fun getDueChars(today: Long, limit: Int): List<String> = dueChars.filter { it !in completedChars() }.take(limit)
        override suspend fun getDueProgress(today: Long, limit: Int): List<HanziProgressEntity> = emptyList()
        override suspend fun getAllChars(): List<String> = (knownChars + completedChars()).distinct()
        override suspend fun learnedCount(): Int = knownChars.size
        override suspend fun dueCount(today: Long): Int = dueChars.size
        override suspend fun getTopWrong(limit: Int): List<HanziProgressEntity> = emptyList()
        override suspend fun getStudyCountsByDay(limit: Int): List<StudyCountRow> = emptyList()
        override suspend fun getAll(): List<HanziProgressEntity> = upsertedEntities
        override suspend fun upsert(entity: HanziProgressEntity) { 
            upsertedEntities.add(entity)
        }
        override suspend fun updateNextDueDay(chars: List<String>, nextDueDay: Long): Int = 0
        override suspend fun resetWrongCount(chars: List<String>): Int = 0
        override suspend fun deleteByChars(chars: List<String>): Int = 0
        override suspend fun deleteAll() {}
    }

    companion object {
        private fun createTestItem(char: String) = CharIndexItem(
            char = char,
            codepoint = char.codePointAt(0),
            file = "$char.json",
            pinyin = listOf("test"),
            strokeCount = 3,
            phrases = listOf("测试$char"),
        )

        private fun createTestCharacterData(char: String, strokeCount: Int = 3) = CharacterData(
            char = char,
            strokes = List(strokeCount) { "M0,0L10,10" },
            medians = List(strokeCount) { listOf(Point(0f, 0f), Point(10f, 10f)) },
        )
    }

    private class FakeCharacterRepository : CharacterRepository {
        private val index = listOf(createTestItem("一"), createTestItem("二"))
        private val characters = mapOf("一" to createTestCharacterData("一"), "二" to createTestCharacterData("二"))

        override suspend fun loadIndex(): List<CharIndexItem> = index
        override suspend fun loadCharacter(item: CharIndexItem): CharacterData = characters[item.char] ?: throw IllegalArgumentException("Character not found")
    }

    private class FakeAppSettingsRepository : AppSettingsRepositoryContract {
        override suspend fun getSettings(): AppSettings = AppSettings()

        override suspend fun updateSettings(settings: AppSettings) {}
    }

    private class FakeDisabledCharRepository : DisabledCharRepositoryContract {
        override suspend fun getAllDisabledChars(): List<String> = emptyList()

        override suspend fun enable(char: String) {}

        override suspend fun disable(char: String) {}
    }

    private class FakePhraseProvider : PracticePhraseProvider {
        override suspend fun phrasesFor(item: CharIndexItem): List<String> = emptyList()
    }

    // Test 1: Initial state is loading false with no data
    @Test
    fun initialState_isNotLoading_withNoData() = runTest {
        val progressRepo = ProgressRepository(
            dao = createFakeDao(),
            timeProvider = SystemTimeProvider,
            policy = DefaultSpacedRepetitionPolicy,
        )
        val viewModel = createViewModel(progressRepo)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.currentCharacter)
            assertNull(state.currentItem)
            assertEquals(0, state.strokeIndex)
            assertEquals(0, state.completedStrokeCount)
        }
    }

    // Test 2: Load practice session initializes state correctly
    @Test
    fun loadPracticeSession_initializesStateCorrectly() = runTest {
        val progressRepo = ProgressRepository(
            dao = createFakeDao(dueChars = listOf("一", "二")),
            timeProvider = SystemTimeProvider,
            policy = DefaultSpacedRepetitionPolicy,
        )
        val viewModel = createViewModel(progressRepo)

        viewModel.onAction(PracticeAction.Start)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("一", state.currentItem?.char)
            assertEquals("一", state.currentCharacter?.char)
            assertEquals(0, state.strokeIndex)
            assertEquals(0, state.completedStrokeCount)
            assertEquals(2, state.windowItems.size)
        }
    }

    // Test 3: Stroke completion advances stroke index
    @Test
    fun strokeCompletion_advancesStrokeIndex() = runTest {
        val progressRepo = ProgressRepository(
            dao = createFakeDao(dueChars = listOf("一")),
            timeProvider = SystemTimeProvider,
            policy = DefaultSpacedRepetitionPolicy,
        )
        val viewModel = createViewModel(progressRepo)

        viewModel.onAction(PracticeAction.Start)
        testDispatcher.scheduler.advanceUntilIdle()

        // Complete first stroke
        viewModel.onAction(PracticeAction.StrokeResult(isMatch = true))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.strokeIndex)
            assertEquals(1, state.completedStrokeCount)
            assertEquals(0, state.mistakesOnStroke)
        }
    }

    // Test 4: Stroke mistake increments mistake count
    @Test
    fun strokeMistake_incrementsMistakeCount() = runTest {
        val progressRepo = ProgressRepository(
            dao = createFakeDao(dueChars = listOf("一")),
            timeProvider = SystemTimeProvider,
            policy = DefaultSpacedRepetitionPolicy,
        )
        val viewModel = createViewModel(progressRepo)

        viewModel.onAction(PracticeAction.Start)
        testDispatcher.scheduler.advanceUntilIdle()

        // Make a mistake
        viewModel.onAction(PracticeAction.StrokeResult(isMatch = false))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.strokeIndex) // Should not advance
            assertEquals(0, state.completedStrokeCount)
            assertEquals(1, state.mistakesOnStroke)
        }
    }

    // Test 5: Character completion switches to next character in window
    @Test
    fun characterCompletion_switchesToNextCharacter() = runTest {
        val progressRepo = ProgressRepository(
            dao = createFakeDao(dueChars = listOf("一", "二")),
            timeProvider = SystemTimeProvider,
            policy = DefaultSpacedRepetitionPolicy,
        )
        val viewModel = createViewModel(progressRepo)

        viewModel.onAction(PracticeAction.Start)
        testDispatcher.scheduler.advanceUntilIdle()

        // Complete all strokes (character complete)
        repeat(3) {
            viewModel.onAction(PracticeAction.StrokeResult(isMatch = true))
            testDispatcher.scheduler.advanceUntilIdle()
        }

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("二", state.currentItem?.char)
            assertEquals("二", state.currentCharacter?.char)
            assertEquals(0, state.strokeIndex)
            assertEquals(0, state.completedStrokeCount)
        }
    }

    // Test 6: Session completion when all characters done
    @Test
    fun sessionCompletion_whenAllCharactersDone() = runTest {
        val progressRepo = ProgressRepository(
            dao = createFakeDao(dueChars = listOf("一")),
            timeProvider = SystemTimeProvider,
            policy = DefaultSpacedRepetitionPolicy,
        )
        val viewModel = createViewModel(progressRepo, reviewOnly = true)

        viewModel.onAction(PracticeAction.Start)
        testDispatcher.scheduler.advanceUntilIdle()

        for (i in 0 until 3) {
            repeat(3) {
                viewModel.onAction(PracticeAction.StrokeResult(isMatch = true))
                testDispatcher.scheduler.advanceUntilIdle()
            }
            testDispatcher.scheduler.advanceUntilIdle()
            if (viewModel.uiState.value.isSessionComplete) break
        }

        val state = viewModel.uiState.value
        assertTrue(state.isSessionComplete)
        assertNull(state.currentItem)
        assertNull(state.currentCharacter)
    }

    // Test 7: Reset stroke state action
    @Test
    fun resetStrokeState_clearsStrokeProgress() = runTest {
        val progressRepo = ProgressRepository(
            dao = createFakeDao(dueChars = listOf("一")),
            timeProvider = SystemTimeProvider,
            policy = DefaultSpacedRepetitionPolicy,
        )
        val viewModel = createViewModel(progressRepo)

        viewModel.onAction(PracticeAction.Start)
        testDispatcher.scheduler.advanceUntilIdle()

        // Complete some strokes
        viewModel.onAction(PracticeAction.StrokeResult(isMatch = true))
        viewModel.onAction(PracticeAction.StrokeResult(isMatch = false))
        testDispatcher.scheduler.advanceUntilIdle()

        // Reset
        viewModel.onAction(PracticeAction.ClearFlash)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(FlashState.None, state.flashColorState)
            assertEquals(1, state.strokeIndex)
            assertEquals(1, state.completedStrokeCount)
            assertEquals(1, state.mistakesOnStroke)
        }
    }
}
