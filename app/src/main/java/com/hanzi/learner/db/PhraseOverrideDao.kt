package com.hanzi.learner.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hanzi.learner.feature.common.extensions.toPhraseList

@Dao
interface PhraseOverrideDao {
    @Query("SELECT * FROM phrase_override WHERE char = :hanziChar LIMIT 1")
    suspend fun getByChar(hanziChar: String): PhraseOverrideEntity?

    @Query("SELECT * FROM phrase_override")
    suspend fun getAll(): List<PhraseOverrideEntity>

    @Query("SELECT COUNT(*) FROM phrase_override")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(entity: PhraseOverrideEntity)

    @Query("DELETE FROM phrase_override WHERE char = :hanziChar")
    suspend fun deleteByChar(hanziChar: String)

    @Query("DELETE FROM phrase_override WHERE char IN (:chars)")
    suspend fun deleteByChars(chars: List<String>): Int

    @Query("DELETE FROM phrase_override")
    suspend fun deleteAll()
}

class PhraseOverrideRepository(
    private val dao: PhraseOverrideDao,
) : PhraseOverrideRepositoryContract {
    override suspend fun getByChar(char: String): PhraseOverrideData? {
        return dao.getByChar(char)?.let {
            PhraseOverrideData(
                char = it.char,
                phrases = it.phrasesJson.toPhraseList(),
            )
        }
    }
}
