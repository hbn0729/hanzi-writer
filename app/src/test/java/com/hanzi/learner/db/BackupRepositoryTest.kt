package com.hanzi.learner.db

import com.hanzi.learner.data.local.dao.AppSettingsDao
import com.hanzi.learner.data.local.dao.DisabledCharDao
import com.hanzi.learner.data.local.dao.HanziProgressDao
import com.hanzi.learner.data.local.dao.PhraseOverrideDao
import com.hanzi.learner.data.local.entity.AppSettingsEntity
import com.hanzi.learner.data.local.entity.DisabledCharEntity
import com.hanzi.learner.data.local.entity.HanziProgressEntity
import com.hanzi.learner.data.local.entity.PhraseOverrideEntity
import com.hanzi.learner.data.local.entity.StudyCountRow
import com.hanzi.learner.data.model.BackupData
import com.hanzi.learner.data.model.ExportOptions
import com.hanzi.learner.data.repository.BackupRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupRepositoryTest {

    @Test
    fun read_withAllOptions_returnsAllData() = runBlocking {
        val progressDao = object : HanziProgressDao {
            override suspend fun getAll(): List<HanziProgressEntity> = listOf(
                HanziProgressEntity(char = "A", correctCount = 1, wrongCount = 0, lastStudiedDay = 1, nextDueDay = 1, intervalDays = 1)
            )
            override suspend fun getByChar(hanziChar: String): HanziProgressEntity? = error("not used")
            override suspend fun getDueChars(today: Long, limit: Int): List<String> = error("not used")
            override suspend fun getDueProgress(today: Long, limit: Int): List<HanziProgressEntity> = error("not used")
            override suspend fun getAllChars(): List<String> = error("not used")
            override suspend fun learnedCount(): Int = error("not used")
            override suspend fun dueCount(today: Long): Int = error("not used")
            override suspend fun getTopWrong(limit: Int): List<HanziProgressEntity> = error("not used")
            override suspend fun getStudyCountsByDay(limit: Int): List<StudyCountRow> = error("not used")
            override suspend fun upsert(entity: HanziProgressEntity) = error("not used")
            override suspend fun updateNextDueDay(chars: List<String>, nextDueDay: Long): Int = error("not used")
            override suspend fun resetWrongCount(chars: List<String>): Int = error("not used")
            override suspend fun deleteByChars(chars: List<String>): Int = error("not used")
            override suspend fun deleteAll() = error("not used")
        }

        val phraseOverrideDao = object : PhraseOverrideDao {
            override suspend fun getAll(): List<PhraseOverrideEntity> = listOf(
                PhraseOverrideEntity(char = "B", phrasesJson = "[\"phrase\"]")
            )
            override suspend fun count(): Int = error("not used")
            override suspend fun upsert(entity: PhraseOverrideEntity) = error("not used")
            override suspend fun deleteAll() = error("not used")
            override suspend fun getByChar(hanziChar: String): PhraseOverrideEntity? = error("not used")
            override suspend fun deleteByChar(hanziChar: String) = error("not used")
            override suspend fun deleteByChars(chars: List<String>): Int = error("not used")
        }

        val disabledCharDao = object : DisabledCharDao {
            override suspend fun getAllDisabledChars(): List<String> = listOf("C")
            override suspend fun count(): Int = error("not used")
            override suspend fun disable(entity: DisabledCharEntity) = error("not used")
            override suspend fun disableAll(entities: List<DisabledCharEntity>) = error("not used")
            override suspend fun enable(hanziChar: String) = error("not used")
            override suspend fun enableAll(chars: List<String>): Int = error("not used")
            override suspend fun deleteAll() = error("not used")
        }

        val appSettingsDao = object : AppSettingsDao {
            override suspend fun get(): AppSettingsEntity? = AppSettingsEntity(useExternalDataset = true)
            override suspend fun upsert(entity: AppSettingsEntity) = error("not used")
        }

        val repo = BackupRepository(
            progressDao = progressDao,
            phraseOverrideDao = phraseOverrideDao,
            disabledCharDao = disabledCharDao,
            appSettingsDao = appSettingsDao,
        )

        val data = repo.read(ExportOptions())

        assertEquals(1, data.version)
        assertEquals(1, data.progress.size)
        assertEquals("A", data.progress[0].char)
        assertEquals(1, data.phraseOverrides.size)
        assertEquals("B", data.phraseOverrides[0].char)
        assertEquals(listOf("C"), data.disabledChars)
        assertEquals(true, data.settings?.useExternalDataset)
    }

    @Test
    fun read_withNoOptions_returnsEmptyData() = runBlocking {
        val progressDao = object : HanziProgressDao {
            override suspend fun getAll(): List<HanziProgressEntity> = error("not used")
            override suspend fun getByChar(hanziChar: String): HanziProgressEntity? = error("not used")
            override suspend fun getDueChars(today: Long, limit: Int): List<String> = error("not used")
            override suspend fun getDueProgress(today: Long, limit: Int): List<HanziProgressEntity> = error("not used")
            override suspend fun getAllChars(): List<String> = error("not used")
            override suspend fun learnedCount(): Int = error("not used")
            override suspend fun dueCount(today: Long): Int = error("not used")
            override suspend fun getTopWrong(limit: Int): List<HanziProgressEntity> = error("not used")
            override suspend fun getStudyCountsByDay(limit: Int): List<StudyCountRow> = error("not used")
            override suspend fun upsert(entity: HanziProgressEntity) = error("not used")
            override suspend fun updateNextDueDay(chars: List<String>, nextDueDay: Long): Int = error("not used")
            override suspend fun resetWrongCount(chars: List<String>): Int = error("not used")
            override suspend fun deleteByChars(chars: List<String>): Int = error("not used")
            override suspend fun deleteAll() = error("not used")
        }

        val phraseOverrideDao = object : PhraseOverrideDao {
            override suspend fun getAll(): List<PhraseOverrideEntity> = error("not used")
            override suspend fun count(): Int = error("not used")
            override suspend fun upsert(entity: PhraseOverrideEntity) = error("not used")
            override suspend fun deleteAll() = error("not used")
            override suspend fun getByChar(hanziChar: String): PhraseOverrideEntity? = error("not used")
            override suspend fun deleteByChar(hanziChar: String) = error("not used")
            override suspend fun deleteByChars(chars: List<String>): Int = error("not used")
        }

        val disabledCharDao = object : DisabledCharDao {
            override suspend fun getAllDisabledChars(): List<String> = error("not used")
            override suspend fun count(): Int = error("not used")
            override suspend fun disable(entity: DisabledCharEntity) = error("not used")
            override suspend fun disableAll(entities: List<DisabledCharEntity>) = error("not used")
            override suspend fun enable(hanziChar: String) = error("not used")
            override suspend fun enableAll(chars: List<String>): Int = error("not used")
            override suspend fun deleteAll() = error("not used")
        }

        val appSettingsDao = object : AppSettingsDao {
            override suspend fun get(): AppSettingsEntity? = error("not used")
            override suspend fun upsert(entity: AppSettingsEntity) = error("not used")
        }

        val repo = BackupRepository(
            progressDao = progressDao,
            phraseOverrideDao = phraseOverrideDao,
            disabledCharDao = disabledCharDao,
            appSettingsDao = appSettingsDao,
        )

        val data = repo.read(ExportOptions(
            progress = false,
            phraseOverrides = false,
            disabledChars = false,
            settings = false,
        ))

        assertEquals(1, data.version)
        assertEquals(emptyList<HanziProgressEntity>(), data.progress)
        assertEquals(emptyList<PhraseOverrideEntity>(), data.phraseOverrides)
        assertEquals(emptyList<String>(), data.disabledChars)
        assertEquals(null, data.settings)
    }

    @Test
    fun replaceAll_deletesAllAndInsertsNewData() = runBlocking {
        var progressDeleted = false
        var phraseDeleted = false
        var disabledDeleted = false
        val insertedProgress = mutableListOf<HanziProgressEntity>()
        val insertedPhrases = mutableListOf<PhraseOverrideEntity>()
        val insertedDisabled = mutableListOf<String>()
        var upsertedSettings: AppSettingsEntity? = null

        val progressDao = object : HanziProgressDao {
            override suspend fun deleteAll() { progressDeleted = true }
            override suspend fun upsert(entity: HanziProgressEntity) { insertedProgress.add(entity) }
            override suspend fun getAll(): List<HanziProgressEntity> = error("not used")
            override suspend fun getByChar(hanziChar: String): HanziProgressEntity? = error("not used")
            override suspend fun getDueChars(today: Long, limit: Int): List<String> = error("not used")
            override suspend fun getDueProgress(today: Long, limit: Int): List<HanziProgressEntity> = error("not used")
            override suspend fun getAllChars(): List<String> = error("not used")
            override suspend fun learnedCount(): Int = error("not used")
            override suspend fun dueCount(today: Long): Int = error("not used")
            override suspend fun getTopWrong(limit: Int): List<HanziProgressEntity> = error("not used")
            override suspend fun getStudyCountsByDay(limit: Int): List<StudyCountRow> = error("not used")
            override suspend fun updateNextDueDay(chars: List<String>, nextDueDay: Long): Int = error("not used")
            override suspend fun resetWrongCount(chars: List<String>): Int = error("not used")
            override suspend fun deleteByChars(chars: List<String>): Int = error("not used")
        }

        val phraseOverrideDao = object : PhraseOverrideDao {
            override suspend fun deleteAll() { phraseDeleted = true }
            override suspend fun upsert(entity: PhraseOverrideEntity) { insertedPhrases.add(entity) }
            override suspend fun getAll(): List<PhraseOverrideEntity> = error("not used")
            override suspend fun count(): Int = error("not used")
            override suspend fun getByChar(hanziChar: String): PhraseOverrideEntity? = error("not used")
            override suspend fun deleteByChar(hanziChar: String) = error("not used")
            override suspend fun deleteByChars(chars: List<String>): Int = error("not used")
        }

        val disabledCharDao = object : DisabledCharDao {
            override suspend fun deleteAll() { disabledDeleted = true }
            override suspend fun disable(entity: DisabledCharEntity) { insertedDisabled.add(entity.char) }
            override suspend fun getAllDisabledChars(): List<String> = error("not used")
            override suspend fun count(): Int = error("not used")
            override suspend fun disableAll(entities: List<DisabledCharEntity>) = error("not used")
            override suspend fun enable(hanziChar: String) = error("not used")
            override suspend fun enableAll(chars: List<String>): Int = error("not used")
        }

        val appSettingsDao = object : AppSettingsDao {
            override suspend fun upsert(entity: AppSettingsEntity) { upsertedSettings = entity }
            override suspend fun get(): AppSettingsEntity? = error("not used")
        }

        val repo = BackupRepository(
            progressDao = progressDao,
            phraseOverrideDao = phraseOverrideDao,
            disabledCharDao = disabledCharDao,
            appSettingsDao = appSettingsDao,
        )

        val backupData = BackupData(
            progress = listOf(HanziProgressEntity(char = "A", correctCount = 1, wrongCount = 0, lastStudiedDay = 1, nextDueDay = 1, intervalDays = 1)),
            phraseOverrides = listOf(PhraseOverrideEntity(char = "B", phrasesJson = "[\"phrase\"]")),
            disabledChars = listOf("C"),
            settings = AppSettingsEntity(useExternalDataset = true),
        )

        repo.replaceAll(backupData)

        assertEquals(true, progressDeleted)
        assertEquals(true, phraseDeleted)
        assertEquals(true, disabledDeleted)
        assertEquals(1, insertedProgress.size)
        assertEquals("A", insertedProgress[0].char)
        assertEquals(1, insertedPhrases.size)
        assertEquals("B", insertedPhrases[0].char)
        assertEquals(listOf("C"), insertedDisabled)
        assertEquals(true, upsertedSettings?.useExternalDataset)
        assertEquals(1, upsertedSettings?.id)
    }

    @Test
    fun mergeAll_insertsDataWithoutDeleting() = runBlocking {
        val insertedProgress = mutableListOf<HanziProgressEntity>()
        val insertedPhrases = mutableListOf<PhraseOverrideEntity>()
        val insertedDisabled = mutableListOf<String>()
        var upsertedSettings: AppSettingsEntity? = null

        val progressDao = object : HanziProgressDao {
            override suspend fun upsert(entity: HanziProgressEntity) { insertedProgress.add(entity) }
            override suspend fun getAll(): List<HanziProgressEntity> = error("not used")
            override suspend fun getByChar(hanziChar: String): HanziProgressEntity? = error("not used")
            override suspend fun getDueChars(today: Long, limit: Int): List<String> = error("not used")
            override suspend fun getDueProgress(today: Long, limit: Int): List<HanziProgressEntity> = error("not used")
            override suspend fun getAllChars(): List<String> = error("not used")
            override suspend fun learnedCount(): Int = error("not used")
            override suspend fun dueCount(today: Long): Int = error("not used")
            override suspend fun getTopWrong(limit: Int): List<HanziProgressEntity> = error("not used")
            override suspend fun getStudyCountsByDay(limit: Int): List<StudyCountRow> = error("not used")
            override suspend fun updateNextDueDay(chars: List<String>, nextDueDay: Long): Int = error("not used")
            override suspend fun resetWrongCount(chars: List<String>): Int = error("not used")
            override suspend fun deleteByChars(chars: List<String>): Int = error("not used")
            override suspend fun deleteAll() = error("not used")
        }

        val phraseOverrideDao = object : PhraseOverrideDao {
            override suspend fun upsert(entity: PhraseOverrideEntity) { insertedPhrases.add(entity) }
            override suspend fun getAll(): List<PhraseOverrideEntity> = error("not used")
            override suspend fun count(): Int = error("not used")
            override suspend fun getByChar(hanziChar: String): PhraseOverrideEntity? = error("not used")
            override suspend fun deleteByChar(hanziChar: String) = error("not used")
            override suspend fun deleteByChars(chars: List<String>): Int = error("not used")
            override suspend fun deleteAll() = error("not used")
        }

        val disabledCharDao = object : DisabledCharDao {
            override suspend fun disable(entity: DisabledCharEntity) { insertedDisabled.add(entity.char) }
            override suspend fun getAllDisabledChars(): List<String> = error("not used")
            override suspend fun count(): Int = error("not used")
            override suspend fun disableAll(entities: List<DisabledCharEntity>) = error("not used")
            override suspend fun enable(hanziChar: String) = error("not used")
            override suspend fun enableAll(chars: List<String>): Int = error("not used")
            override suspend fun deleteAll() = error("not used")
        }

        val appSettingsDao = object : AppSettingsDao {
            override suspend fun upsert(entity: AppSettingsEntity) { upsertedSettings = entity }
            override suspend fun get(): AppSettingsEntity? = error("not used")
        }

        val repo = BackupRepository(
            progressDao = progressDao,
            phraseOverrideDao = phraseOverrideDao,
            disabledCharDao = disabledCharDao,
            appSettingsDao = appSettingsDao,
        )

        val backupData = BackupData(
            progress = listOf(HanziProgressEntity(char = "A", correctCount = 1, wrongCount = 0, lastStudiedDay = 1, nextDueDay = 1, intervalDays = 1)),
            phraseOverrides = listOf(PhraseOverrideEntity(char = "B", phrasesJson = "[\"phrase\"]")),
            disabledChars = listOf("C"),
            settings = AppSettingsEntity(useExternalDataset = true),
        )

        repo.mergeAll(backupData)

        assertEquals(1, insertedProgress.size)
        assertEquals("A", insertedProgress[0].char)
        assertEquals(1, insertedPhrases.size)
        assertEquals("B", insertedPhrases[0].char)
        assertEquals(listOf("C"), insertedDisabled)
        assertEquals(true, upsertedSettings?.useExternalDataset)
        assertEquals(1, upsertedSettings?.id)
    }
}