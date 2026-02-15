package com.hanzi.learner.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

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
