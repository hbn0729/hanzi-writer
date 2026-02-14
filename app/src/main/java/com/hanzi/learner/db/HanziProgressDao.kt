package com.hanzi.learner.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface HanziProgressDao {
    @Query("SELECT * FROM hanzi_progress WHERE char = :hanziChar LIMIT 1")
    suspend fun getByChar(hanziChar: String): HanziProgressEntity?

    @Query("SELECT char FROM hanzi_progress WHERE nextDueDay <= :today ORDER BY nextDueDay ASC LIMIT :limit")
    suspend fun getDueChars(today: Long, limit: Int): List<String>

    @Query("SELECT * FROM hanzi_progress WHERE nextDueDay <= :today ORDER BY nextDueDay ASC LIMIT :limit")
    suspend fun getDueProgress(today: Long, limit: Int): List<HanziProgressEntity>

    @Query("SELECT char FROM hanzi_progress ORDER BY lastStudiedDay DESC")
    suspend fun getAllChars(): List<String>

    @Query("SELECT COUNT(*) FROM hanzi_progress")
    suspend fun learnedCount(): Int

    @Query("SELECT COUNT(*) FROM hanzi_progress WHERE nextDueDay <= :today")
    suspend fun dueCount(today: Long): Int

    @Query("SELECT * FROM hanzi_progress ORDER BY wrongCount DESC, lastStudiedDay DESC LIMIT :limit")
    suspend fun getTopWrong(limit: Int): List<HanziProgressEntity>

    @Query(
        "SELECT lastStudiedDay AS day, COUNT(*) AS count " +
            "FROM hanzi_progress " +
            "WHERE lastStudiedDay > 0 " +
            "GROUP BY lastStudiedDay " +
            "ORDER BY day DESC " +
            "LIMIT :limit"
    )
    suspend fun getStudyCountsByDay(limit: Int): List<StudyCountRow>

    @Query("SELECT * FROM hanzi_progress")
    suspend fun getAll(): List<HanziProgressEntity>

    @Upsert
    suspend fun upsert(entity: HanziProgressEntity)

    @Query("UPDATE hanzi_progress SET nextDueDay = :nextDueDay WHERE char IN (:chars)")
    suspend fun updateNextDueDay(chars: List<String>, nextDueDay: Long): Int

    @Query("UPDATE hanzi_progress SET wrongCount = 0 WHERE char IN (:chars)")
    suspend fun resetWrongCount(chars: List<String>): Int

    @Query("DELETE FROM hanzi_progress WHERE char IN (:chars)")
    suspend fun deleteByChars(chars: List<String>): Int

    @Query("DELETE FROM hanzi_progress")
    suspend fun deleteAll()
}
