package dev.akinom.isod

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform