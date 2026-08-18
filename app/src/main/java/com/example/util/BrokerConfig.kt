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
}

