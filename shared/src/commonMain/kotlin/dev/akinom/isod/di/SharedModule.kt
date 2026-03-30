package dev.akinom.isod.di

import app.cash.sqldelight.db.SqlDriver
import dev.akinom.isod.IsodDatabase
import dev.akinom.isod.Secrets
import dev.akinom.isod.auth.CredentialsStorage
import dev.akinom.isod.auth.IsodAuthRepository
import dev.akinom.isod.auth.createSettings
import dev.akinom.isod.auth.currentSemester
import dev.akinom.isod.data.cache.DatabaseDriverFactory
import dev.akinom.isod.data.remote.AkinomApiClient
import dev.akinom.isod.data.remote.IsodApiClient
import dev.akinom.isod.data.remote.UsosApiClient
import dev.akinom.isod.data.repository.AcademicCalendarRepository
import dev.akinom.isod.data.repository.CourseRepository
import dev.akinom.isod.data.repository.EventRepository
import dev.akinom.isod.data.repository.GradesRepository
import dev.akinom.isod.data.repository.NewsRepository
import dev.akinom.isod.data.repository.PlanRepository
import dev.akinom.isod.data.repository.TimetableRepository
import dev.akinom.isod.data.repository.UsosRepository
import dev.akinom.isod.notifications.FirstLaunchGuard
import dev.akinom.isod.notifications.NewsNotificationChecker
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule = module {

    single { createSettings() }
    single { CredentialsStorage(get()) }

    single {
        createHttpClient().config {
            install(Logging) {
                level = LogLevel.INFO
                logger = object : Logger {
                    override fun log(message: String) = println("🌍 Ktor: $message")
                }
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                })
            }
        }
    }

    single<SqlDriver> {
        val factory = get<DatabaseDriverFactory>()
        val driver = try {
            factory.createDriver()
        } catch (e: Exception) {
            println("❌ Database driver creation failed, deleting and recreating: ${e.message}")
            factory.deleteDatabase()
            factory.createDriver()
        }

        try {
            val db = IsodDatabase(driver)
            db.newsQueries.selectAllHeaders("").executeAsList()
            db.eventQueries.selectAll().executeAsList()
            db.academicCalendarQueries.selectAllExams().executeAsList()
            driver
        } catch (e: Exception) {
            println("❌ Database schema validation failed (likely new tables), deleting and recreating: ${e.message}")
            try { driver.close() } catch (_: Exception) {}
            factory.deleteDatabase()
            factory.createDriver()
        }
    }

    single { IsodDatabase(get()) }

    single<CoroutineScope> { CoroutineScope(Dispatchers.Default + SupervisorJob()) }

    single { IsodAuthRepository(get()) }

    single {
        IsodApiClient(
            httpClient = get(),
            storage    = get(),
        )
    }

    single { AkinomApiClient(get()) }

    single { PlanRepository(get(), get(), get()) }
    single { NewsRepository(get(), get(), get()) }
    single { CourseRepository(get(), get(), get()) }
    single { AcademicCalendarRepository(get(), get(), get()) }
    single { EventRepository(get(), get(), get()) }

    single {
        UsosApiClient(
            httpClient     = get(),
            storage        = get(),
            consumerKey    = Secrets.USOS_CONSUMER_KEY,
            consumerSecret = Secrets.USOS_CONSUMER_SECRET,
        )
    }

    single { UsosRepository(get(), get(), get()) }
    single { TimetableRepository(get(), get(), get(), get(), get()) }
    single { GradesRepository(get(), get(), get(), get()) }
}

expect fun createHttpClient(): HttpClient

val notificationModule = module {
    single {
        NewsNotificationChecker(
            db = get(),
            isodApi = get(),
            storage = get(),
            notificationService = get(),
            semester = currentSemester()
        )
    }
    single { FirstLaunchGuard(get(), get()) }
}

expect val platformModule: Module
