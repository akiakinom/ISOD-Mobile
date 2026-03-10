package dev.akinom.isod.di

import dev.akinom.isod.IsodDatabase
import dev.akinom.isod.Secrets
import dev.akinom.isod.auth.CredentialsStorage
import dev.akinom.isod.auth.createSettings
import dev.akinom.isod.data.cache.DatabaseDriverFactory
import dev.akinom.isod.data.remote.IsodApiClient
import dev.akinom.isod.data.remote.UsosApiClient
import dev.akinom.isod.data.repository.CourseRepository
import dev.akinom.isod.data.repository.GradesRepository
import dev.akinom.isod.data.repository.NewsRepository
import dev.akinom.isod.data.repository.PlanRepository
import dev.akinom.isod.data.repository.TimetableRepository
import dev.akinom.isod.data.repository.UsosRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule = module {

    single { createSettings() }
    single { CredentialsStorage(get()) }

    single {
        HttpClient {
            install(Logging) {
                level = LogLevel.NONE
                logger = object : Logger {
                    override fun log(message: String) = println("🌍 Ktor: $message")
                }
            }
        }
    }

    single { get<DatabaseDriverFactory>().createDriver() }
    single { IsodDatabase(get()) }

    single<CoroutineScope> { CoroutineScope(Dispatchers.Default + SupervisorJob()) }

    single {
        val storage = get<CredentialsStorage>()
        IsodApiClient(
            httpClient = get(),
            username   = storage.getIsodUsername() ?: "",
            apiKey     = storage.getIsodApiKey()   ?: "",
        )
    }

    single { PlanRepository(get(), get(), get()) }
    single { NewsRepository(get(), get(), get()) }
    single { CourseRepository(get(), get(), get()) }

    single {
        UsosApiClient(
            httpClient     = get(),
            storage        = get(),
            consumerKey    = Secrets.USOS_CONSUMER_KEY,
            consumerSecret = Secrets.USOS_CONSUMER_SECRET,
        )
    }

    single { UsosRepository(get(), get(), get()) }
    single { TimetableRepository(get(), get(), get()) }
    single { GradesRepository(get(), get()) }

}

expect val platformModule: Module
