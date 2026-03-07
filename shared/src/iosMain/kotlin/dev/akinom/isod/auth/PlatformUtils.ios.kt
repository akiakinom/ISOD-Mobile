package dev.akinom.isod.auth

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeSeconds(): Long =
    NSDate().timeIntervalSince1970.toLong()

actual fun ByteArray.toHexString(): String =
    joinToString("") { it.toInt().and(0xff).toString(16).padStart(2, '0') }