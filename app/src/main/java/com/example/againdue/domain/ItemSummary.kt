package com.example.againdue.domain

import java.time.LocalDate

/**
 * The minimal item shape the home list needs: identity, label, due date,
 * archived state, and whether the recurrence has ended. Persistence entities
 * map into this so sorting stays free of Room and Android types.
 */
data class ItemSummary(
    val id: Long,
    val name: String,
    val nextDueAt: LocalDate,
    val archived: Boolean = false,
    val ended: Boolean = false,
)
