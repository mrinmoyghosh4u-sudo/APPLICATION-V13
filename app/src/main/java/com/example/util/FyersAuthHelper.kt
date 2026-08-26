package com.example.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

object FyersAuthHelper {
    const val DEFAULT_REDIRECT_URI = "https://application-beige-psi.vercel.app/oauth"
    const val FALLBACK_REDIRECT_URI = "https://trade.fyers.in/api-login/redirect-uri/index.html"

    fun generateSecureState(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        val randomHex = bytes.joinToString("") { "%02x".format(it) }
        return "fyers_$randomHex"
    }

    fun getFullAppId(appId: String): String {
        val trimmed = appId.trim()
        return if (trimmed.isNotBlank() && !trimmed.contains("-100")) "$trimmed-100" else trimmed
    }

    fun generateAppIdHash(appId: String, secretId: String): String {
        val fullAppId = getFullAppId(appId)
        val input = "$fullAppId:$secretId"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun buildLoginUrl(
        appId: String,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        state: String? = null
    ): String {
        val fullAppId = getFullAppId(appId)
        val encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString())
        var url = "https://api-t1.fyers.in/api/v3/generate-authcode?client_id=$fullAppId&redirect_uri=$encodedRedirect&response_type=code"
        if (!state.isNullOrBlank()) {
            val encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8.toString())
            url += "&state=$encodedState"
        }
        return url
    }
}
