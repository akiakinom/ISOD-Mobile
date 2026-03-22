package dev.akinom.isod.onboarding.usos

import androidx.compose.runtime.Composable

@Composable
expect fun UsosWebView(
    url: String,
    onCallbackReceived: (verifier: String) -> Unit,
    onError: (message: String) -> Unit,
)