package dev.akinom.isod.di

import dev.akinom.isod.data.cache.DatabaseDriverFactory
import dev.akinom.isod.notifications.NotificationService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { NotificationService(androidContext()) }
}