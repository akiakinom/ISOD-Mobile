package dev.akinom.isod

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.Navigator
import dev.akinom.isod.auth.AppThemeSetting
import dev.akinom.isod.auth.CredentialsStorage
import dev.akinom.isod.auth.createSettings
import dev.akinom.isod.news.NewsDetailScreen
import dev.akinom.isod.notifications.FirstLaunchGuard
import dev.akinom.isod.onboarding.isod.ISODLinkScreen
import dev.akinom.isod.ui.theme.AppTheme
import org.koin.mp.KoinPlatform.getKoin

@Composable
fun App(
    initialTab: MainTab? = null,
    initialDayOfWeek: Int? = null,
    initialNewsHash: String? = null
) {
    val firstLaunchGuard = remember { getKoin().get<FirstLaunchGuard>() }
    LaunchedEffect(Unit) {
        firstLaunchGuard.runIfNeeded()
    }

    val storage = remember { CredentialsStorage(createSettings()) }
    var themeSetting by remember { mutableStateOf(storage.getTheme()) }

    val startScreen = remember {
        when {
            !storage.hasIsodCredentials() -> ISODLinkScreen()
            initialNewsHash != null        -> NewsDetailScreen(initialNewsHash)
            else                         -> MainScreen(initialTab, initialDayOfWeek)
        }
    }

    val darkTheme = when (themeSetting) {
        AppThemeSetting.SYSTEM -> isSystemInDarkTheme()
        AppThemeSetting.LIGHT -> false
        AppThemeSetting.DARK -> true
    }

    AppTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(LocalThemeSetting provides { theme -> 
            storage.setTheme(theme)
            themeSetting = theme
        }) {
            Navigator(startScreen)
        }
    }
}

val LocalThemeSetting = staticCompositionLocalOf<(AppThemeSetting) -> Unit> {
    { _ -> }
}
