package dev.akinom.isod.data.cache

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.akinom.isod.IsodDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(IsodDatabase.Schema, context, "isod.db")

    actual fun deleteDatabase() {
        context.deleteDatabase("isod.db")
    }
}