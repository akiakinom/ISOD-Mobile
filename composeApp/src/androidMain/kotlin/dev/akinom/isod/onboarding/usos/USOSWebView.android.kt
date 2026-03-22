// composeApp/src/androidMain/kotlin/dev/akinom/isod/UsosWebView.android.kt
package dev.akinom.isod.onboarding.usos

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.akinom.isod.auth.USOS_CALLBACK_URL

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun UsosWebView(
    url: String,
    onCallbackReceived: (verifier: String) -> Unit,
    onError: (message: String) -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var webProgress by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    // Enable cookies and third-party cookies for CAS login redirects
                    CookieManager.getInstance().let {
                        it.setAcceptCookie(true)
                        it.setAcceptThirdPartyCookies(this, true)
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        @Suppress("DEPRECATION")
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        
                        // Use a standard mobile User Agent.
                        // Sometimes forced Desktop UAs cause USOS to hide content if the screen is detected as mobile.
                        userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                    }
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            webProgress = newProgress
                            if (newProgress == 100) isLoading = false
                        }

                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            Log.d("USOSWebView", "JS Console: ${consoleMessage?.message()} [${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()}]")
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            Log.d("USOSWebView", "Page Started: $url")
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            Log.d("USOSWebView", "Page Finished: $url")
                            isLoading = false
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            val requestUrl = request.url.toString()
                            Log.d("USOSWebView", "Navigating: $requestUrl")
                            
                            if (requestUrl.startsWith(USOS_CALLBACK_URL)) {
                                val verifier = request.url.getQueryParameter("oauth_verifier")
                                if (verifier != null) {
                                    onCallbackReceived(verifier)
                                } else {
                                    val error = request.url.getQueryParameter("error") ?: "Authorization denied"
                                    onError(error)
                                }
                                return true
                            }
                            return false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                Log.e("USOSWebView", "Main Frame Error: ${error?.description} (${error?.errorCode})")
                            }
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler?,
                            error: SslError?
                        ) {
                            Log.e("USOSWebView", "SSL Error: ${error?.primaryError}")
                            super.onReceivedSslError(view, handler, error)
                        }
                    }
                    loadUrl(url)
                }
            },
            update = { _ -> }
        )

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { webProgress / 100f },
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}