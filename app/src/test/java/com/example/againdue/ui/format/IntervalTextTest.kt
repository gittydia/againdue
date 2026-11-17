package com.example.againdue.ui.format

import com.example.againdue.domain.IntervalUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalTextTest {

    @Test
    fun `single intervals read naturally`() {
        assertEquals("Every day", intervalLabel(1, IntervalUnit.DAY))
        assertEquals("Every week", intervalLabel(1, IntervalUnit.WEEK))
        assertEquals("Every month", intervalLabel(1, IntervalUnit.MONTH))
    }

    @Test
    fun `plural intervals include the count`() {
        assertEquals("Every 3 days", intervalLabel(3, IntervalUnit.DAY))
        assertEquals("Every 6 weeks", intervalLabel(6, IntervalUnit.WEEK))
        assertEquals("Every 12 months", intervalLabel(12, IntervalUnit.MONTH))
    }

    @Test
    fun `unit labels are plural`() {
        assertEquals("Days", intervalUnitLabel(IntervalUnit.DAY))
        assertEquals("Weeks", intervalUnitLabel(IntervalUnit.WEEK))
        assertEquals("Months", intervalUnitLabel(IntervalUnit.MONTH))
    }
}
