package com.hanzi.learner.feature.home.viewmodel

import com.hanzi.learner.db.AppSettings
import com.hanzi.learner.db.AppSettingsRepositoryContract
import com.hanzi.learner.db.DisabledCharRepositoryContract
import com.hanzi.learner.db.DefaultSpacedRepetitionPolicy
import com.hanzi.learner.db.ProgressRepository
import com.hanzi.learner.db.ProgressRepositoryContract
import com.hanzi.learner.db.SystemTimeProvider
import com.hanzi.learner.db.HanziProgressDao
import com.hanzi.learner.db.HanziProgressEntity
import com.hanzi.learner.db.StudyCountRow
import com.hanzi.learner.feature.common.ports.CharacterRepositoryProvider
import com.hanzi.learner.feature.home.domain.LoadHomeDataUseCase
import com.hanzi.learner.feature.home.domain.ResolveHomeCharacterRepositoryUseCase
import com.hanzi.learner.hanzi.data.CharIndexItem
import com.hanzi.learner.hanzi.data.CharacterRepository
import com.hanzi.learner.hanzi.model.CharacterData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelTest {

    private fun item(char: String) = CharIndexItem(
        char = char,
        codepoint = char.codePointAt(0),
        file = "$char.json",
        pinyin = emptyList(),
        strokeCount = 1,
        phrases = emptyList(),
    )

    private class FakeHanziProgressDao(
        private val reviewCount: Int,
        private val learnedChars: List<String>,
    ) : HanziProgressDao {
        override suspend fun getByChar(hanziChar: String): HanziProgressEntity? = null
        override suspend fun getDueChars(today: Long, limit: Int): List<String> = emptyList()
        override suspend fun getDueProgress(today: Long, limit: Int): List<HanziProgressEntity> = emptyList()
        override suspend fun getAllChars(): List<String> = learnedChars
        override suspend fun learnedCount(): Int = learnedChars.size
        override suspend fun dueCount(today: Long): Int = reviewCount
        override suspend fun getTopWrong(limit: Int): List<HanziProgressEntity> = emptyList()
        override suspend fun getStudyCountsByDay(limit: Int): List<StudyCountRow> = emptyList()
        override suspend fun getAll(): List<HanziProgressEntity> = emptyList()
        override suspend fun upsert(entity: HanziProgressEntity) {}
        override suspend fun updateNextDueDay(chars: List<String>, nextDueDay: Long): Int = 0
        override suspend fun resetWrongCount(chars: List<String>): Int = 0
        override suspend fun deleteByChars(chars: List<String>): Int = 0
        override suspend fun deleteAll() {}
    }

    private fun createProgressRepository(reviewCount: Int, learnedChars: List<String>): ProgressRepository {
        return ProgressRepository(
            dao = FakeHanziProgressDao(reviewCount, learnedChars),
            timeProvider = SystemTimeProvider,
            policy = DefaultSpacedRepetitionPolicy,
        )
    }

    private fun createDisabledCharRepository(disabled: List<String>) = object : DisabledCharRepositoryContract {
        override suspend fun getAllDisabledChars(): List<String> = disabled

        override suspend fun enable(char: String) = error("not used")

        override suspend fun disable(char: String) = error("not used")
    }

    private fun createCharacterRepository(index: List<CharIndexItem>) = object : CharacterRepository {
        override suspend fun loadIndex(): List<CharIndexItem> = index
        override suspend fun loadCharacter(item: CharIndexItem): CharacterData = error("not used")
    }

    private fun createViewModel(
        progressRepository: ProgressRepositoryContract,
        disabledChars: List<String>,
        characterRepository: CharacterRepository,
        navigationCallback: (HomeNavigation) -> Unit = {},
    ): HomeViewModel {
        val settingsRepository = object : AppSettingsRepositoryContract {
            override suspend fun getSettings(): AppSettings = AppSettings(useExternalDataset = false)
            override suspend fun updateSettings(settings: AppSettings) = Unit
        }
        val provider = object : CharacterRepositoryProvider {
            override fun get(useExternalDataset: Boolean): CharacterRepository = characterRepository
        }
        val resolveRepoUseCase = ResolveHomeCharacterRepositoryUseCase(settingsRepository, provider)
        val loadHomeDataUseCase = LoadHomeDataUseCase(
            progressRepository = progressRepository,
            disabledCharRepository = createDisabledCharRepository(disabledChars),
            resolveCharacterRepositoryUseCase = resolveRepoUseCase,
        )
        return HomeViewModel(
            navigationCallback = navigationCallback,
            loadHomeDataUseCase = loadHomeDataUseCase,
        )
    }

    @Test
    fun initialState_isDefault() = runBlocking {
        val viewModel = createViewModel(
            progressRepository = createProgressRepository(0, emptyList()),
            disabledChars = emptyList(),
            characterRepository = createCharacterRepository(emptyList()),
        )

        val state = viewModel.uiState.first()
        assertEquals(HomeUiState(isLoading = false, data = null, error = null), state)
    }

    @Test
    fun loadData_updatesStateWithCounts() = runBlocking {
        val viewModel = createViewModel(
            progressRepository = createProgressRepository(3, listOf("A")),
            disabledChars = emptyList(),
            characterRepository = createCharacterRepository(listOf(item("A"), item("B"), item("C"))),
        )

        viewModel.onAction(HomeAction.LoadData)

        // Wait for state update
        val state = viewModel.uiState.first { it.data != null }
        assertEquals(HomeUiState(isLoading = false, data = HomeData(unlearnedCount = 2, reviewCount = 3), error = null), state)
    }

    @Test
    fun loadData_excludesDisabledCharsFromUnlearnedCount() = runBlocking {
        val viewModel = createViewModel(
            progressRepository = createProgressRepository(0, emptyList()),
            disabledChars = listOf("B"),
            characterRepository = createCharacterRepository(listOf(item("A"), item("B"), item("C"))),
        )

        viewModel.onAction(HomeAction.LoadData)

        val state = viewModel.uiState.first { it.data != null }
        assertEquals(HomeUiState(isLoading = false, data = HomeData(unlearnedCount = 2, reviewCount = 0), error = null), state)
    }

    @Test
    fun loadData_setsErrorWhenExceptionThrown() = runBlocking {
        val viewModel = createViewModel(
            progressRepository = createProgressRepository(0, emptyList()),
            disabledChars = emptyList(),
            characterRepository = object : CharacterRepository {
                override suspend fun loadIndex(): List<CharIndexItem> = throw IllegalStateException("boom")
                override suspend fun loadCharacter(item: CharIndexItem): CharacterData = error("not used")
            },
        )

        viewModel.onAction(HomeAction.LoadData)

        val state = viewModel.uiState.first { it.error != null }
        assertEquals(false, state.isLoading)
        assertEquals(null, state.data)
        assertEquals("boom", state.error)
    }

    @Test
    fun selectMode_emitsNavigation() = runBlocking {
        val navigations = mutableListOf<HomeNavigation>()

        val viewModel = createViewModel(
            progressRepository = createProgressRepository(0, emptyList()),
            disabledChars = emptyList(),
            characterRepository = createCharacterRepository(emptyList()),
            navigationCallback = { navigations.add(it) },
        )

        viewModel.onAction(HomeAction.SelectMode(PracticeMode.PRACTICE))
        viewModel.onAction(HomeAction.SelectMode(PracticeMode.REVIEW))

        assertEquals(listOf(HomeNavigation.NavigateToPractice, HomeNavigation.NavigateToReview), navigations)
    }
}
