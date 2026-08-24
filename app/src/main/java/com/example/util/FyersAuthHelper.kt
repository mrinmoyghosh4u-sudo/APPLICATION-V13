package com.example.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object FyersAuthHelper {
    const val DEFAULT_REDIRECT_URI = "https://application-beige-psi.vercel.app/oauth"
    const val FALLBACK_REDIRECT_URI = "https://trade.fyers.in/api-login/redirect-uri/index.html"

    fun generateAppIdHash(appId: String, secretId: String): String {
        val input = "$appId:$secretId"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun buildLoginUrl(
        appId: String,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        state: String? = null
    ): String {
        val encodedRedirect = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString())
        var url = "https://api-t1.fyers.in/api/v3/generate-authcode?client_id=$appId&redirect_uri=$encodedRedirect&response_type=code"
        if (!state.isNullOrBlank()) {
            val encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8.toString())
            url += "&state=$encodedState"
        }
        return url
    }
}
