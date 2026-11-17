package com.example.againdue.data.local

import androidx.room.TypeConverter
import com.example.againdue.domain.IntervalUnit
import java.time.Instant
import java.time.LocalDate

/**
 * Room type converters for the value types used by the domain.
 *
 * Dates are stored as epoch days and timestamps as epoch milliseconds so the
 * schema stays portable and does not depend on the device time zone.
 */
class Converters {

    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun fromEpochMilli(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun toEpochMilli(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun fromIntervalUnit(value: String?): IntervalUnit? = value?.let(IntervalUnit::valueOf)

    @TypeConverter
    fun toIntervalUnit(value: IntervalUnit?): String? = value?.name
}
