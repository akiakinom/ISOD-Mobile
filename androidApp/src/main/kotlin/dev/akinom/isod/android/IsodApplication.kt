package dev.akinom.isod.android

import android.app.Application
import dev.akinom.isod.android.widget.WidgetUpdater
import dev.akinom.isod.auth.CredentialsStorage
import dev.akinom.isod.auth.LessonServiceControl
import dev.akinom.isod.auth.PlatformLessonService
import dev.akinom.isod.auth.PlatformWidgetUpdater
import dev.akinom.isod.auth.TimetableWidgetUpdater
import dev.akinom.isod.auth.initSettingsContext
import dev.akinom.isod.di.initKoin
import dev.akinom.isod.di.notificationModule
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext

class IsodApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initSettingsContext(this)
        
        TimetableWidgetUpdater.provider = object : PlatformWidgetUpdater {
            override fun updateAllWidgets() {
                WidgetUpdater.updateAllWidgets(this@IsodApplication)
            }
        }

        LessonServiceControl.provider = object : PlatformLessonService {
            override fun startService() {
                LessonService.start(this@IsodApplication)
            }
            override fun stopService() {
                LessonService.stop(this@IsodApplication)
            }
        }

        initKoin(additionalModules = listOf(notificationModule)) {
            androidContext(this@IsodApplication)
        }

        val storage: CredentialsStorage = get()
        if (storage.isOngoingLessonNotifEnabled()) {
            LessonServiceControl.start()
        }
    }
}
