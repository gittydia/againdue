package com.example.againdue.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.againdue.domain.IntervalUnit
import com.example.againdue.domain.RecurrencePattern
import java.time.Instant
import java.time.LocalDate

/**
 * A recurring item.
 *
 * [lastDoneAt] is the source of truth for the schedule; [nextDueAt] is stored
 * only so queries and list rendering stay cheap, and it is always recomputed
 * from the recurrence definition whenever either the interval or the last
 * completion changes.
 *
 * [startDate] is the anchor for calendar patterns such as "the first Monday of
 * every month". It is nullable so items created before recurrences existed keep
 * their completion-based arithmetic; a null value falls back to [lastDoneAt].
 *
 * [recurrenceEnded] is set once an item passes [endDate] or reaches
 * [endAfterCount] completions. The item stays visible, but reminders stop and
 * it can no longer be completed.
 *
 * [reminderMinuteOfDay] overrides the global reminder time when present. It is
 * the number of minutes after local midnight, from 0 through 1439.
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
    val startDate: LocalDate? = null,
    val pattern: RecurrencePattern? = null,
    val endDate: LocalDate? = null,
    val endAfterCount: Int? = null,
    @ColumnInfo(defaultValue = "0") val recurrenceEnded: Boolean = false,
    @ColumnInfo(defaultValue = "0") val completedCount: Int = 0,
    val reminderMinuteOfDay: Int? = null,
)
