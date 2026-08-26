package com.example.util

import java.net.URLEncoder
import java.security.SecureRandom

object UpstoxAuthHelper {

    const val DEFAULT_REDIRECT_URI = "https://application-beige-psi.vercel.app/oauth"
    private const val AUTH_DIALOG_BASE = "https://api-v2.upstox.com/v2/login/authorization/dialog"

    fun generateSecureState(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        val randomHex = bytes.joinToString("") { "%02x".format(it) }
        return "upstox_$randomHex"
    }

    fun getAuthorizationUrl(
        apiKey: String,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        state: String? = null
    ): String {
        val encodedRedirect = URLEncoder.encode(redirectUri, "UTF-8")
        var url = "$AUTH_DIALOG_BASE?response_type=code&client_id=$apiKey&redirect_uri=$encodedRedirect"
        if (!state.isNullOrBlank()) {
            val encodedState = URLEncoder.encode(state, "UTF-8")
            url += "&state=$encodedState"
        }
        return url
    }

    fun buildLoginUrl(
        apiKey: String,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        state: String? = null
    ): String = getAuthorizationUrl(apiKey, redirectUri, state)
}
