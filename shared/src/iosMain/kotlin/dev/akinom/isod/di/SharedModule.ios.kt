package dev.akinom.isod.di

import dev.akinom.isod.data.cache.DatabaseDriverFactory
import dev.akinom.isod.notifications.NotificationService
import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseDriverFactory() }
    single { NotificationService() }
}

actual fun createHttpClient(): HttpClient {
    return HttpClient(Darwin)
}
