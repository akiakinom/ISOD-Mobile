package dev.akinom.isod.auth

expect fun currentTimeSeconds(): Long
expect fun ByteArray.toHexString(): String

expect fun currentWeekMonday(): String
expect fun currentSemester(): String
expect fun currentDayOfWeek(): Int
expect fun currentTimeHHmm(): String
expect fun getAppVersion(): String
