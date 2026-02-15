package com.hanzi.learner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DisabledCharDao {
    @Query("SELECT char FROM disabled_char")
    suspend fun getAllDisabledChars(): List<String>

    @Query("SELECT COUNT(*) FROM disabled_char")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun disable(entity: DisabledCharEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun disableAll(entities: List<DisabledCharEntity>)

    @Query("DELETE FROM disabled_char WHERE char = :hanziChar")
    suspend fun enable(hanziChar: String)

    @Query("DELETE FROM disabled_char WHERE char IN (:chars)")
    suspend fun enableAll(chars: List<String>): Int

    @Query("DELETE FROM disabled_char")
    suspend fun deleteAll()
}
