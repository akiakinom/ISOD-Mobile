package dev.akinom.isod.android

import android.app.Application
import dev.akinom.isod.auth.initSettingsContext
import dev.akinom.isod.di.initKoin
import org.koin.android.ext.koin.androidContext

class IsodApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initSettingsContext(this)
        initKoin {
            androidContext(this@IsodApplication)
        }
    }
}
