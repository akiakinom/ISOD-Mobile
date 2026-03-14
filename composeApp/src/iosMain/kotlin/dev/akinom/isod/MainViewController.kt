package dev.akinom.isod

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController(
    initialTab: MainTab? = null,
    initialDayOfWeek: Int? = null,
    initialNewsHash: String? = null
) = ComposeUIViewController {
    App(
        initialTab = initialTab,
        initialDayOfWeek = initialDayOfWeek,
        initialNewsHash = initialNewsHash
    )
}
