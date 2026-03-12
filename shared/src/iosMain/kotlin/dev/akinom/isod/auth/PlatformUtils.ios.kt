package dev.akinom.isod.auth

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierISO8601
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSBundle
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeSeconds(): Long =
    NSDate().timeIntervalSince1970.toLong()

actual fun ByteArray.toHexString(): String =
    joinToString("") { it.toInt().and(0xff).toString(16).padStart(2, '0') }

actual fun currentWeekMonday(): String {
    val cal = NSCalendar(NSCalendarIdentifierISO8601)
    val now = NSDate()
    val components = cal.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitWeekday,
        fromDate = now,
    )
    val weekday = components.weekday.toInt()
    val diff = (weekday - 2 + 7) % 7
    val monday = cal.dateByAddingUnit(
        NSCalendarUnitDay,
        value = -diff.toLong(),
        toDate = now,
        options = 0u,
    ) ?: now
    val mc = cal.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay,
        fromDate = monday,
    )
    val y = mc.year.toString().padStart(4, '0')
    val m = mc.month.toString().padStart(2, '0')
    val d = mc.day.toString().padStart(2, '0')
    return "$y-$m-$d"
}

actual fun currentSemester(): String {
    val cal = NSCalendar(NSCalendarIdentifierISO8601)
    val now = NSDate()
    val components = cal.components(NSCalendarUnitYear or NSCalendarUnitMonth, fromDate = now)
    val year = components.year.toInt()
    val month = components.month.toInt()

    return when (month) {
        1 -> "${year - 1}Z"
        in 2..9 -> "${year}L"
        else -> "${year}Z"
    }
}

actual fun currentDayOfWeek(): Int {
    val cal = NSCalendar(NSCalendarIdentifierISO8601)
    val now = NSDate()
    val components = cal.components(NSCalendarUnitWeekday, fromDate = now)
    val weekday = components.weekday.toInt()
    
    return when (weekday) {
        2 -> 1 // Mon
        3 -> 2 // Tue
        4 -> 3 // Wed
        5 -> 4 // Thu
        6 -> 5 // Fri
        7 -> 6 // Sat
        1 -> 7 // Sun
        else -> 1
    }
}

actual fun currentTimeHHmm(): String {
    val cal = NSCalendar(NSCalendarIdentifierISO8601)
    val now = NSDate()
    val components = cal.components(NSCalendarUnitHour or NSCalendarUnitMinute, fromDate = now)
    val h = components.hour.toString().padStart(2, '0')
    val m = components.minute.toString().padStart(2, '0')
    return "$h:$m"
}

actual fun getAppVersion(): String {
    return NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: "1.0.0"
}
