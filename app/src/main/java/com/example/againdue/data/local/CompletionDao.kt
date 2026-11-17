package com.example.againdue.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CompletionDao {

    @Insert
    suspend fun insert(completion: Completion): Long

    @Query("SELECT * FROM completions WHERE itemId = :itemId ORDER BY completedAt DESC, id DESC LIMIT 1")
    suspend fun latestForItem(itemId: Long): Completion?

    @Query("SELECT * FROM completions WHERE itemId = :itemId ORDER BY completedAt DESC, id DESC")
    suspend fun historyForItem(itemId: Long): List<Completion>

    @Query("SELECT COUNT(*) FROM completions WHERE itemId = :itemId")
    suspend fun countForItem(itemId: Long): Int

    @Query("DELETE FROM completions WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
