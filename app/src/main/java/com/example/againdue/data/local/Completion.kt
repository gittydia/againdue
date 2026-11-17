package com.example.againdue.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * An audit record for one completion of an item.
 *
 * Keeping the previous completion date makes `complete` and `undo` atomic and
 * lets the most recent completion be reversed without guessing the old state.
 */
@Entity(
    tableName = "completions",
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("itemId")],
)
data class Completion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val completedAt: Instant,
    val previousLastDoneAt: LocalDate,
)
