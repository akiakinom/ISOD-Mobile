package dev.akinom.isod.data.cache

object CacheConfig {
    const val PLAN_TTL_MS              = 60 * 60 * 1000L        // 1 hour
    const val NEWS_TTL_MS              = 15 * 60 * 1000L        // 15 minutes
    const val COURSES_TTL_MS           = 60 * 60 * 1000L        // 1 hour
    const val CLASS_TTL_MS             = 10 * 60 * 1000L        // 10 minutes
    const val ACADEMIC_CALENDAR_TTL_MS = 24 * 60 * 60 * 1000L   // 1 day
}

fun isStale(lastUpdatedMs: Long, ttlMs: Long): Boolean {
    val now = currentTimeMillis()
    return now - lastUpdatedMs > ttlMs
}

expect fun currentTimeMillis(): Long
