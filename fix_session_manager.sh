cat << 'INNER' > app/src/main/java/com/example/data/network/SessionManager.kt
package com.example.data.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "kingkhan_trade_prefs_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("kingkhan_trade_prefs_v2", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_ACTIVE_BROKER = "active_broker"
        private const val KEY_ANGEL_JWT = "angel_jwt_token_enc"
        private const val KEY_ANGEL_REFRESH = "angel_refresh_token_enc"
        private const val KEY_ANGEL_FEED = "angel_feed_token_enc"
        private const val KEY_ANGEL_CLIENT_ID = "angel_client_id"
        private const val KEY_ANGEL_API_KEY = "angel_api_key_enc"
        private const val KEY_DHAN_TOKEN = "dhan_access_token_enc"
        private const val KEY_DHAN_CLIENT_ID = "dhan_client_id"
        private const val KEY_TELEGRAM_BOT_TOKEN = "telegram_bot_token"
        private const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"
        private const val KEY_TELEGRAM_ALERTS_ENABLED = "telegram_alerts_enabled"
    }

    private fun encrypt(value: String?): String? = value
    private fun decrypt(value: String?): String? = value

    var activeBroker: String
        get() = prefs.getString(KEY_ACTIVE_BROKER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACTIVE_BROKER, value).apply()

    var angelJwtToken: String?
        get() = decrypt(prefs.getString(KEY_ANGEL_JWT, null))
        set(value) = prefs.edit().putString(KEY_ANGEL_JWT, encrypt(value)).apply()

    var angelRefreshToken: String?
        get() = decrypt(prefs.getString(KEY_ANGEL_REFRESH, null))
        set(value) = prefs.edit().putString(KEY_ANGEL_REFRESH, encrypt(value)).apply()

    var angelFeedToken: String?
        get() = decrypt(prefs.getString(KEY_ANGEL_FEED, null))
        set(value) = prefs.edit().putString(KEY_ANGEL_FEED, encrypt(value)).apply()

    var angelClientId: String
        get() = prefs.getString(KEY_ANGEL_CLIENT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANGEL_CLIENT_ID, value).apply()

    var angelApiKey: String
        get() {
            val saved = decrypt(prefs.getString(KEY_ANGEL_API_KEY, null))
            if (!saved.isNullOrEmpty()) return saved ?: ""
            val envKey: String? = runCatching<String?> { com.example.BuildConfig.ANGEL_ONE_API_KEY as String? }.getOrNull()
            if (!envKey.isNullOrBlank() && envKey != "ANGEL_ONE_API_KEY_DEFAULT_VALUE") return envKey ?: ""
            return ""
        }
        set(value) = prefs.edit().putString(KEY_ANGEL_API_KEY, encrypt(value)).apply()

    var dhanAccessToken: String?
        get() {
            val saved = decrypt(prefs.getString(KEY_DHAN_TOKEN, null))
            if (!saved.isNullOrEmpty()) return saved
            val envToken: String? = runCatching<String?> { com.example.BuildConfig.DHAN_API_KEY as String? }.getOrNull()
            if (!envToken.isNullOrBlank() && envToken != "DHAN_API_KEY_DEFAULT_VALUE") return envToken
            return null
        }
        set(value) = prefs.edit().putString(KEY_DHAN_TOKEN, encrypt(value)).apply()

    var dhanClientId: String
        get() = prefs.getString(KEY_DHAN_CLIENT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DHAN_CLIENT_ID, value).apply()

    var telegramBotToken: String
        get() = prefs.getString(KEY_TELEGRAM_BOT_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TELEGRAM_BOT_TOKEN, value).apply()

    var telegramChatId: String
        get() = prefs.getString(KEY_TELEGRAM_CHAT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TELEGRAM_CHAT_ID, value).apply()

    var isTelegramAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TELEGRAM_ALERTS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_TELEGRAM_ALERTS_ENABLED, value).apply()

    fun getRecentSearches(): List<String> {
        val raw = prefs.getString("recent_searches_list", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|||").filter { it.isNotBlank() }
    }

    fun addRecentSearch(query: String) {
        val clean = query.trim().uppercase()
        if (clean.isBlank()) return
        val current = getRecentSearches().toMutableList()
        current.remove(clean)
        current.add(0, clean)
        val trimmed = current.take(20)
        prefs.edit().putString("recent_searches_list", trimmed.joinToString("|||")).apply()
    }

    fun clearRecentSearches() {
        prefs.edit().remove("recent_searches_list").apply()
    }

    fun hasValidSession(): Boolean {
        return when (activeBroker) {
            "Angel One" -> !angelJwtToken.isNullOrBlank()
            "Dhan" -> !dhanAccessToken.isNullOrBlank()
            else -> !angelJwtToken.isNullOrBlank() || !dhanAccessToken.isNullOrBlank()
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
INNER
