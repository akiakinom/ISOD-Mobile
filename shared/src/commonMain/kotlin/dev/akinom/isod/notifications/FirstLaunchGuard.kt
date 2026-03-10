package dev.akinom.isod.notifications

import com.russhwolf.settings.Settings

private const val KEY_NOTIFICATIONS_BOOTSTRAPPED = "notifications_bootstrapped"

/**
 * On the very first run after install, we don't want to fire notifications
 * for all historical news items already in the DB. This guard marks all
 * existing items as sent once, then never runs again.
 */
class FirstLaunchGuard(
    private val settings: Settings,
    private val checker: NewsNotificationChecker,
) {
    fun runIfNeeded() {
        val alreadyDone = settings.getBoolean(KEY_NOTIFICATIONS_BOOTSTRAPPED, false)
        if (!alreadyDone) {
            checker.suppressHistorical()
            settings.putBoolean(KEY_NOTIFICATIONS_BOOTSTRAPPED, true)
            println("✅ FirstLaunchGuard: suppressed historical notifications")
        }
    }
}