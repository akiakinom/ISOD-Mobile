package dev.akinom.isod.auth

expect fun currentTimeSeconds(): Long
expect fun ByteArray.toHexString(): String

expect fun currentWeekMonday(): String
expect fun currentSemester(): String
