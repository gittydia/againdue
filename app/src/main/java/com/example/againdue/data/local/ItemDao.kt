package com.example.againdue.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ItemDao {

    /** Non-archived items; ordering is applied by the domain sorter. */
    @Query("SELECT * FROM items WHERE archived = 0")
    fun observeActive(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE archived = 1")
    fun observeArchived(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun observeById(id: Long): Flow<Item?>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): Item?

    @Query("SELECT * FROM items")
    suspend fun getAll(): List<Item>

    @Insert
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Query("UPDATE items SET archived = :archived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, updatedAt: Instant): Int

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
