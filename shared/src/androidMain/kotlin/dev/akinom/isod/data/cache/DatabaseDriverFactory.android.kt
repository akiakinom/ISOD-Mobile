package dev.akinom.isod.data.cache

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.akinom.isod.ISODMobileDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(
            schema = ISODMobileDatabase.Schema,
            context = context,
            name = "isod.db",
            callback = object : AndroidSqliteDriver.Callback(ISODMobileDatabase.Schema) {
                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {
                    recreate(db)
                }

                override fun onDowngrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {
                    recreate(db)
                }

                private fun recreate(db: SupportSQLiteDatabase) {
                    val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")
                    val tables = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        tables.add(cursor.getString(0))
                    }
                    cursor.close()

                    tables.forEach { tableName ->
                        db.execSQL("DROP TABLE IF EXISTS \"$tableName\"")
                    }

                    onCreate(db)
                }
            }
        )

    actual fun deleteDatabase() {
        context.deleteDatabase("isod.db")
    }
}
