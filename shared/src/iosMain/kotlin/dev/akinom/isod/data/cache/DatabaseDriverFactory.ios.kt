package dev.akinom.isod.data.cache

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import dev.akinom.isod.IsodDatabase
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
}
