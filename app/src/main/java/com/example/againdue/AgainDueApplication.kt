package com.example.againdue

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AgainDueApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container.reminderNotifications.ensureChannel()
        applicationScope.launch {
            container.reminderScheduler.syncAll()
        }
    }
}
