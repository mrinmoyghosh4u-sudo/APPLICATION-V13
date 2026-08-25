package com.example.util

import com.example.BuildConfig

object BrokerConfig {
    val dhanClientId: String
        get() = runCatching { BuildConfig.DHAN_CLIENT_ID }.getOrNull()
            ?.takeIf { it != "DHAN_CLIENT_ID_DEFAULT_VALUE" && it.isNotBlank() }
            ?: System.getenv("DHAN_CLIENT_ID")
            ?: ""

    val dhanApiKey: String
        get() = runCatching { BuildConfig.DHAN_API_KEY }.getOrNull()
            ?.takeIf { it != "DHAN_API_KEY_DEFAULT_VALUE" && it.isNotBlank() }
            ?: System.getenv("DHAN_API_KEY")
            ?: ""

    val dhanClientSecret: String
        get() = runCatching { BuildConfig.DHAN_CLIENT_SECRET }.getOrNull()
            ?.takeIf { it != "DHAN_CLIENT_SECRET_DEFAULT_VALUE" && it.isNotBlank() }
            ?: System.getenv("DHAN_CLIENT_SECRET")
            ?: ""

    val dhanRedirectUri: String
        get() = runCatching { BuildConfig.DHAN_REDIRECT_URI }.getOrNull()
            ?.takeIf { it != "DHAN_REDIRECT_URI_DEFAULT_VALUE" && it.isNotBlank() }
            ?: System.getenv("DHAN_REDIRECT_URI")
            ?: "kingkhan://oauth/callback"

    val angelApiKey: String
        get() = runCatching { BuildConfig.ANGEL_ONE_API_KEY }.getOrNull()
            ?.takeIf { it != "ANGEL_ONE_API_KEY_DEFAULT_VALUE" && it.isNotBlank() }
            ?: System.getenv("ANGEL_ONE_API_KEY")
            ?: ""

    val angelRedirectUri: String
        get() = runCatching { BuildConfig.ANGEL_REDIRECT_URI }.getOrNull()
            ?.takeIf { it != "ANGEL_REDIRECT_URI_DEFAULT_VALUE" && it.isNotBlank() }
            ?: System.getenv("ANGEL_REDIRECT_URI")
            ?: "kingkhan://oauth/callback"

    val upstoxApiKey: String
        get() = runCatching { BuildConfig.UPSTOX_API_KEY }.getOrNull()
            ?.takeIf { it != "UPSTOX_API_KEY_DEFAULT_VALUE" && it.isNotBlank() }
            ?: System.getenv("UPSTOX_API_KEY")
            ?: ""

    val upstoxApiSecret: String
        get() = runCatching { BuildConfig.UPSTOX_API_SECRET }.getOrNull()
            ?.takeIf { it != "UPSTOX_API_SECRET_DEFAULT_VALUE" && it.isNotBlank() }
            ?: System.getenv("UPSTOX_API_SECRET")
            ?: ""

    val upstoxRedirectUri: String
        get() = runCatching { BuildConfig.UPSTOX_REDIRECT_URI }.getOrNull()
            ?.takeIf { it != "UPSTOX_REDIRECT_URI_DEFAULT_VALUE" && it.isNotBlank() }
            ?: System.getenv("UPSTOX_REDIRECT_URI")
            ?: "https://application-beige-psi.vercel.app/oauth"

    val fyersAppId: String
        get() = runCatching { BuildConfig.FYERS_APP_ID }.getOrNull()
            ?.takeIf { it != "FYERS_APP_ID_DEFAULT_VALUE" && it.isNotBlank() }
            ?: System.getenv("FYERS_APP_ID")
            ?: ""

    val fyersSecretId: String
        get() = runCatching { BuildConfig.FYERS_SECRET_ID }.getOrNull()
            ?.takeIf { it != "FYERS_SECRET_ID_DEFAULT_VALUE" && it.isNotBlank() }
            ?: System.getenv("FYERS_SECRET_ID")
            ?: ""

    val fyersRedirectUri: String
        get() = runCatching { BuildConfig.FYERS_REDIRECT_URI }.getOrNull()
            ?.takeIf { it != "FYERS_REDIRECT_URI_DEFAULT_VALUE" && it.isNotBlank() }
            ?: System.getenv("FYERS_REDIRECT_URI")
            ?: "https://application-beige-psi.vercel.app/oauth"
}

