package dev.akinom.isod.data.cache

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun List<String>.encodeToString(): String = json.encodeToString(this)

fun String.decodeStringList(): List<String> =
    runCatching { json.decodeFromString<List<String>>(this) }.getOrDefault(emptyList())
