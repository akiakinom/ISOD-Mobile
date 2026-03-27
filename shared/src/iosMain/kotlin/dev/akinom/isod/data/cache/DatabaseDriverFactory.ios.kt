package dev.akinom.isod.data.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.akinom.isod.IsodDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val appGroup = "group.dev.akinom.isod"
        val manager = NSFileManager.defaultManager
        val containerUrl = manager.containerURLForSecurityApplicationGroupIdentifier(appGroup)
        
        return NativeSqliteDriver(
            schema = IsodDatabase.Schema,
            name = "isod.db",
            onConfiguration = { config ->
                config.copy(
                    extendedConfig = config.extendedConfig.copy(
                        basePath = containerUrl?.path
                    )
                )
            }
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun deleteDatabase() {
        val appGroup = "group.dev.akinom.isod"
        val manager = NSFileManager.defaultManager
        val containerUrl = manager.containerURLForSecurityApplicationGroupIdentifier(appGroup)
        val databaseUrl = containerUrl?.URLByAppendingPathComponent("isod.db")
        databaseUrl?.path?.let { path ->
            if (manager.fileExistsAtPath(path)) {
                manager.removeItemAtPath(path, null)
            }
        }
    }
}
