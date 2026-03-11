package dev.akinom.isod.auth
import java.util.Calendar

actual fun currentTimeSeconds(): Long = System.currentTimeMillis() / 1000

actual fun ByteArray.toHexString(): String =
    joinToString("") { "%02x".format(it) }

actual fun currentWeekMonday(): String {
    val cal = Calendar.getInstance().apply {
        // Move to Monday (Calendar.MONDAY = 2)
        val dow = get(Calendar.DAY_OF_WEEK)
        val diff = (dow - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_MONTH, -diff)
    }
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1
    val d = cal.get(Calendar.DAY_OF_MONTH)
    return "%04d-%02d-%02d".format(y, m, d)
}

actual fun currentSemester(): String {
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1 // Calendar.JANUARY is 0

    return when (month) {
        1 -> "${year - 1}Z"
        in 2..9 -> "${year}L"
        else -> "${year}Z"
    }
}
