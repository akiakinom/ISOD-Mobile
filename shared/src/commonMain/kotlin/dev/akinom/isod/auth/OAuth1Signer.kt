package dev.akinom.isod.auth

import io.ktor.http.*
import org.kotlincrypto.macs.hmac.sha1.HmacSHA1
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

object OAuth1Signer {

    private const val OAUTH_VERSION          = "1.0"
    private const val OAUTH_SIGNATURE_METHOD = "HMAC-SHA1"

    @OptIn(ExperimentalEncodingApi::class)
    fun buildHeader(
        method: String,
        url: String,
        consumerKey: String,
        consumerSecret: String,
        token: String? = null,
        tokenSecret: String? = null,
        extraParams: Map<String, String> = emptyMap(),
    ): String {
        val timestamp = currentTimeSeconds().toString()
        val nonce     = Random.nextBytes(16).toHexString()

        val oauthParams = mutableMapOf(
            "oauth_consumer_key"     to consumerKey,
            "oauth_nonce"            to nonce,
            "oauth_signature_method" to OAUTH_SIGNATURE_METHOD,
            "oauth_timestamp"        to timestamp,
            "oauth_version"          to OAUTH_VERSION,
        )
        if (token != null) oauthParams["oauth_token"] = token

        val allParams = (oauthParams + extraParams).entries
            .sortedBy { it.key }
            .joinToString("&") { "${encode(it.key)}=${encode(it.value)}" }

        val baseString = listOf(
            method.uppercase(),
            encode(url),
            encode(allParams),
        ).joinToString("&")

        val signingKey = "${encode(consumerSecret)}&${encode(tokenSecret ?: "")}"

        val mac = HmacSHA1(signingKey.encodeToByteArray())
        val signature = Base64.encode(mac.doFinal(baseString.encodeToByteArray()))

        oauthParams["oauth_signature"] = signature

        return "OAuth " + oauthParams.entries
            .sortedBy { it.key }
            .joinToString(", ") { "${encode(it.key)}=\"${encode(it.value)}\"" }
    }

    private fun encode(value: String): String = value.encodeURLParameter()
}