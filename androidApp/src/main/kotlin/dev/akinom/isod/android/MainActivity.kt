package dev.akinom.isod.android

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.akinom.isod.App
import dev.akinom.isod.MainTab
import dev.akinom.isod.android.widget.WidgetUpdater

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        println(if (granted) "✅ Notification permission granted" else "❌ Notification permission denied")
    }

    private var initialTab by mutableStateOf<MainTab?>(null)
    private var initialDayOfWeek by mutableStateOf<Int?>(null)
    private var initialNewsHash by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        handleIntent()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        NewsNotificationWorker.schedule(this)
        WidgetUpdater.updateAllWidgets(this)

        setContent {
            App(
                initialTab = initialTab,
                initialDayOfWeek = initialDayOfWeek,
                initialNewsHash = initialNewsHash
            )
        }
    }

    override fun onResume() {
        super.onResume()
        WidgetUpdater.updateAllWidgets(this)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent()
        WidgetUpdater.updateAllWidgets(this)
    }

    private fun handleIntent() {
        val tabName = intent.getStringExtra("tab")
        if (tabName != null) {
            initialTab = MainTab.entries.find { it.name == tabName }
        }
        val dayOfWeek = intent.getIntExtra("dayOfWeek", -1)
        if (dayOfWeek != -1) {
            initialDayOfWeek = dayOfWeek
        } else {
            initialDayOfWeek = null
        }
        
        initialNewsHash = intent.getStringExtra("newsHash")
    }
}
