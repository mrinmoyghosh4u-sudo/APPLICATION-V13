package com.example.util

import com.example.BuildConfig

object BrokerConfig {
    val dhanClientId: String
        get() = BuildConfig.DHAN_CLIENT_ID.takeIf { it != "DHAN_CLIENT_ID_DEFAULT_VALUE" } ?: ""

    val dhanApiKey: String
        get() = BuildConfig.DHAN_API_KEY.takeIf { it != "DHAN_API_KEY_DEFAULT_VALUE" } ?: ""

    val dhanClientSecret: String
        get() = BuildConfig.DHAN_CLIENT_SECRET.takeIf { it != "DHAN_CLIENT_SECRET_DEFAULT_VALUE" } ?: ""

    val dhanRedirectUri: String = "kingkhan://oauth/callback"

    val angelApiKey: String
        get() = BuildConfig.ANGEL_ONE_API_KEY.takeIf { it != "ANGEL_ONE_API_KEY_DEFAULT_VALUE" } ?: ""

    val angelRedirectUri: String = "kingkhan://oauth/callback"
}
