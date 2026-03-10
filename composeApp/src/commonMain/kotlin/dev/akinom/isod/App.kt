package dev.akinom.isod

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.Navigator
import dev.akinom.isod.auth.CredentialsStorage
import dev.akinom.isod.auth.createSettings
import dev.akinom.isod.home.HomeScreen
import dev.akinom.isod.notifications.FirstLaunchGuard
import dev.akinom.isod.onboarding.isod.IsodLinkScreen
import org.koin.mp.KoinPlatform.getKoin

@Composable
fun App() {
    val firstLaunchGuard = remember { getKoin().get<FirstLaunchGuard>() }
    LaunchedEffect(Unit) {
        firstLaunchGuard.runIfNeeded()
    }

    val storage = remember { CredentialsStorage(createSettings()) }

    val startScreen = remember {
        when {
            storage.hasIsodCredentials() -> HomeScreen()
            else                         -> IsodLinkScreen()
        }
    }

    MaterialTheme {
        Navigator(startScreen)
    }
}