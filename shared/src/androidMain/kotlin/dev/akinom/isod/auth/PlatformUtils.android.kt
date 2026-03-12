package dev.akinom.isod.auth
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun currentTimeSeconds(): Long = System.currentTimeMillis() / 1000

actual fun ByteArray.toHexString(): String =
    joinToString("") { "%02x".format(it) }

actual fun currentWeekMonday(): String {
    val cal = Calendar.getInstance().apply {
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
    val month = cal.get(Calendar.MONTH) + 1

    return when (month) {
        1 -> "${year - 1}Z"
        in 2..9 -> "${year}L"
        else -> "${year}Z"
    }
}

actual fun currentDayOfWeek(): Int {
    val cal = Calendar.getInstance()
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    return when (dow) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }
}

actual fun currentTimeHHmm(): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Calendar.getInstance().time)
}

object AppVersionProvider : KoinComponent {
    val context: Context by inject()
    fun getVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}

actual fun getAppVersion(): String = AppVersionProvider.getVersion()
