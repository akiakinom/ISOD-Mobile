package dev.akinom.isod.di

import dev.akinom.isod.data.cache.DatabaseDriverFactory
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseDriverFactory() }
}