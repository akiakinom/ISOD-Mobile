package dev.akinom.isod.onboarding.usos

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import dev.akinom.isod.auth.USOS_CALLBACK_URL
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.NSURLRequest
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun UsosWebView(
    url: String,
    onCallbackReceived: (verifier: String) -> Unit,
) {
    val navigationDelegate = remember {
        object : NSObject(), WKNavigationDelegateProtocol {
            override fun webView(
                webView: WKWebView,
                decidePolicyForNavigationAction: WKNavigationAction,
                decisionHandler: (WKNavigationActionPolicy) -> Unit
            ) {
                val nsUrl = decidePolicyForNavigationAction.request.URL
                
                if (nsUrl != null && nsUrl.absoluteString?.startsWith(USOS_CALLBACK_URL) == true) {
                    val components = NSURLComponents.componentsWithURL(nsUrl, resolvingAgainstBaseURL = false)
                    val verifier = components?.queryItems?.filterIsInstance<NSURLQueryItem>()
                        ?.firstOrNull { it.name == "oauth_verifier" }
                        ?.value

                    if (verifier != null) {
                        onCallbackReceived(verifier)
                        decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
                        return
                    }
                }
                decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
            }
        }
    }

    UIKitView(
        factory = {
            val config = WKWebViewConfiguration()
            WKWebView(frame = CGRectZero.readValue(), configuration = config).apply {
                this.navigationDelegate = navigationDelegate
                val nsUrl = NSURL.URLWithString(url)
                if (nsUrl != null) {
                    loadRequest(NSURLRequest.requestWithURL(nsUrl))
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { _ -> }
    )
}
