package com.example.againdue.ui.item

import com.example.againdue.domain.IntervalUnit
import java.time.LocalDate

data class ItemFormErrors(
    val name: String? = null,
    val interval: String? = null,
) {
    val hasErrors: Boolean
        get() = name != null || interval != null
}

data class ItemFormState(
    val name: String = "",
    val intervalText: String = "",
    val unit: IntervalUnit = IntervalUnit.DAY,
    val lastDoneAt: LocalDate = LocalDate.now(),
    val note: String = "",
) {
    fun intervalValueOrNull(): Int? = intervalText.trim().toIntOrNull()

    fun errors(): ItemFormErrors = ItemFormErrors(
        name = if (name.isBlank()) "Enter a name" else null,
        interval = when (val value = intervalValueOrNull()) {
            null -> if (intervalText.isBlank()) {
                "Enter how often this repeats"
            } else {
                "Use a whole number"
            }

            else -> if (value <= 0) "Interval must be at least 1" else null
        },
    )
}
