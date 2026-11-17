package com.example.againdue.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.againdue.notification.ReminderScheduler
import com.example.againdue.notification.ReminderSettingsStore
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: ReminderSettingsStore,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    val settings = settingsStore.settings

    fun setEnabled(enabled: Boolean) {
        settingsStore.setEnabled(enabled)
        reschedule()
    }

    fun setTime(hour: Int, minute: Int) {
        settingsStore.setTime(hour, minute)
        reschedule()
    }

    private fun reschedule() {
        viewModelScope.launch { scheduler.syncAll() }
    }
}
