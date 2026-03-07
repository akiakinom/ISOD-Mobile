// composeApp/src/androidMain/kotlin/dev/akinom/isod/UsosWebView.android.kt
package dev.akinom.isod.onboarding.usos

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import dev.akinom.isod.auth.USOS_CALLBACK_URL

@Composable
actual fun UsosWebView(
    url: String,
    onCallbackReceived: (verifier: String) -> Unit,
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        val requestUrl = request.url.toString()
                        if (requestUrl.startsWith(USOS_CALLBACK_URL)) {
                            // Extract oauth_verifier from callback URL
                            val verifier = request.url
                                .getQueryParameter("oauth_verifier")
                                ?: return true
                            onCallbackReceived(verifier)
                            return true
                        }
                        return false
                    }
                }
                loadUrl(url)
            }
        }
    )
}