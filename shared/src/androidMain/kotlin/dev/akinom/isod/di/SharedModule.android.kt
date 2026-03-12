package dev.akinom.isod.di

import dev.akinom.isod.data.cache.DatabaseDriverFactory
import dev.akinom.isod.notifications.NotificationService
import io.ktor.client.*
import io.ktor.client.engine.android.*
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

actual val platformModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { NotificationService(androidContext()) }
}

actual fun createHttpClient(): HttpClient {
    return HttpClient(Android) {
        engine {
            sslManager = { connection ->
                if (connection is HttpsURLConnection) {
                    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                    })
                    val sslContext = SSLContext.getInstance("SSL")
                    sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                    connection.sslSocketFactory = sslContext.socketFactory
                    connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
                }
            }
        }
    }
}
