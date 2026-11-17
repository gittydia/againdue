package com.example.againdue

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.againdue.ui.AgainDueApp
import com.example.againdue.ui.theme.AgainDueTheme

class MainActivity : ComponentActivity() {

    private var openedItemId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openedItemId = intent.reminderItemId()
        val container = (application as AgainDueApplication).container
        setContent {
            AgainDueTheme {
                AgainDueApp(
                    container = container,
                    openedItemId = openedItemId,
                    onOpenedItemHandled = { openedItemId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openedItemId = intent.reminderItemId()
    }
}

private fun Intent.reminderItemId(): Long? {
    if (action != Intent.ACTION_VIEW) return null
    val uri = data ?: return null
    if (uri.scheme != "againdue" || uri.host != "item") return null
    return uri.pathSegments.firstOrNull()?.toLongOrNull()
}
