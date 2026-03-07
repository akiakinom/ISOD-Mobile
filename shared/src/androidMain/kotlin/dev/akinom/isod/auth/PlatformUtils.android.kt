package dev.akinom.isod.auth

actual fun currentTimeSeconds(): Long = System.currentTimeMillis() / 1000

actual fun ByteArray.toHexString(): String =
    joinToString("") { "%02x".format(it) }
