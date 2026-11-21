package com.example.againdue.ui.item

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ItemFormStateTest {

    @Test
    fun defaultReminderTimeInheritsSettings() {
        val form = ItemFormState(reminderMinuteOfDay = 9 * 60)

        assertNull(form.reminderTimeOverride())
    }

    @Test
    fun customReminderTimeReturnsMinuteOfDay() {
        val form = ItemFormState(
            useCustomReminderTime = true,
            reminderMinuteOfDay = 21 * 60 + 5,
        )

        assertEquals(21 * 60 + 5, form.reminderTimeOverride())
    }
}
