package com.example.againdue.ui.item

import com.example.againdue.domain.IntervalUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemFormStateTest {

    @Test
    fun `blank form reports both errors`() {
        val errors = ItemFormState().errors()

        assertEquals("Enter a name", errors.name)
        assertEquals("Enter how often this repeats", errors.interval)
        assertTrue(errors.hasErrors)
    }

    @Test
    fun `blank interval asks how often`() {
        val errors = ItemFormState(name = "Passport").errors()

        assertNull(errors.name)
        assertEquals("Enter how often this repeats", errors.interval)
    }

    @Test
    fun `non numeric interval is rejected`() {
        val errors = ItemFormState(name = "Passport", intervalText = "six").errors()

        assertEquals("Use a whole number", errors.interval)
    }

    @Test
    fun `zero and negative intervals are rejected`() {
        assertEquals("Interval must be at least 1", ItemFormState(intervalText = "0").errors().interval)
        assertEquals("Interval must be at least 1", ItemFormState(intervalText = "-3").errors().interval)
    }

    @Test
    fun `valid form has no errors and a parsed interval`() {
        val state = ItemFormState(
            name = "Passport",
            intervalText = " 6 ",
            unit = IntervalUnit.MONTH,
        )

        assertEquals(6, state.intervalValueOrNull())
        assertFalse(state.errors().hasErrors)
    }
}
