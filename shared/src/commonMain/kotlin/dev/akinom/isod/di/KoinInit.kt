package dev.akinom.isod.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

private var isKoinInitialized = false

fun initKoin(additionalModules: List<Module> = emptyList(), appDeclaration: KoinApplication.() -> Unit = {}) {
    if (isKoinInitialized) return
    
    startKoin {
        appDeclaration()
        modules(listOf(sharedModule, platformModule) + additionalModules)
    }
    isKoinInitialized = true
}
