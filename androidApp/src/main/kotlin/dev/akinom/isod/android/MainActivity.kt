package dev.akinom.isod.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.akinom.isod.App
import dev.akinom.isod.auth.initSettingsContext
import dev.akinom.isod.di.initKoin
import org.koin.android.ext.koin.androidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initSettingsContext(this)
        initKoin {
            androidContext(this@MainActivity)
        }

        setContent { App() }
    }
}
