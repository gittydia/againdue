package com.example.againdue.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.againdue.domain.IntervalUnit
import java.time.Instant
import java.time.LocalDate

/**
 * A recurring item.
 *
 * [lastDoneAt] is the source of truth for the schedule; [nextDueAt] is stored
 * only so queries and list rendering stay cheap, and it is always recomputed
 * from [lastDoneAt] plus the interval whenever either changes.
 */
@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val intervalValue: Int,
    val intervalUnit: IntervalUnit,
    val lastDoneAt: LocalDate,
    val nextDueAt: LocalDate,
    val note: String? = null,
    val archived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)
