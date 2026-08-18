package com.example.data.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {
    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        initPrefs(appContext)
    }

    private fun initPrefs(context: Context): SharedPreferences {
        val prefFileName = "kingkhan_secure_session_v4"
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                prefFileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("SessionManager", "EncryptedSharedPreferences init failed: ${e.message}, cleaning and recreating")
            runCatching {
                context.deleteSharedPreferences(prefFileName)
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    prefFileName,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }.getOrElse {
                Log.e("SessionManager", "Fallback to standard SharedPreferences")
                context.getSharedPreferences("kingkhan_trade_prefs_std", Context.MODE_PRIVATE)
            }
        }
    }

    companion object {
        private const val KEY_ACTIVE_BROKER = "active_broker"
        private const val KEY_ANGEL_JWT = "angel_jwt_token_enc"
        private const val KEY_ANGEL_REFRESH = "angel_refresh_token_enc"
        private const val KEY_ANGEL_FEED = "angel_feed_token_enc"
        private const val KEY_ANGEL_CLIENT_ID = "angel_client_id"
        private const val KEY_ANGEL_API_KEY = "angel_api_key_enc"
        private const val KEY_ANGEL_TOKEN_TIME = "angel_token_time"
        private const val KEY_DHAN_TOKEN = "dhan_access_token_enc"
        private const val KEY_DHAN_CLIENT_ID = "dhan_client_id"
        private const val KEY_DHAN_TOKEN_TIME = "dhan_token_time"
        private const val KEY_TELEGRAM_BOT_TOKEN = "telegram_bot_token"
        private const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"
        private const val KEY_TELEGRAM_ALERTS_ENABLED = "telegram_alerts_enabled"
    }

    private fun safeGetToken(key: String): String? {
        val raw = prefs.getString(key, null)
        if (raw.isNullOrBlank()) return null
        return raw
    }

    private fun safeSetToken(key: String, value: String?) {
        if (!value.isNullOrBlank()) {
            prefs.edit().putString(key, value.trim()).commit()
        }
    }

    var activeBroker: String
        get() = prefs.getString(KEY_ACTIVE_BROKER, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_ACTIVE_BROKER, value).commit()
        }

    var connectedBroker: String
        get() = activeBroker
        set(value) { activeBroker = value }

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean("is_biometric_enabled", false)
        set(value) {
            prefs.edit().putBoolean("is_biometric_enabled", value).commit()
        }

    fun hasActiveSession(): Boolean = hasValidSession()

    var angelJwtToken: String?
        get() = safeGetToken(KEY_ANGEL_JWT)
        set(value) {
            safeSetToken(KEY_ANGEL_JWT, value)
        }

    var angelRefreshToken: String?
        get() = safeGetToken(KEY_ANGEL_REFRESH)
        set(value) {
            safeSetToken(KEY_ANGEL_REFRESH, value)
        }

    var angelFeedToken: String?
        get() = safeGetToken(KEY_ANGEL_FEED)
        set(value) {
            safeSetToken(KEY_ANGEL_FEED, value)
        }

    var angelClientId: String
        get() = prefs.getString(KEY_ANGEL_CLIENT_ID, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_ANGEL_CLIENT_ID, value).commit()
        }

    var angelMpin: String
        get() = safeGetToken("angel_mpin_enc") ?: ""
        set(value) {
            safeSetToken("angel_mpin_enc", value)
        }

    var angelTokenTimestamp: Long
        get() = prefs.getLong(KEY_ANGEL_TOKEN_TIME, 0L)
        set(value) {
            prefs.edit().putLong(KEY_ANGEL_TOKEN_TIME, value).commit()
        }

    var angelApiKey: String
        get() {
            val saved = safeGetToken(KEY_ANGEL_API_KEY)
            if (!saved.isNullOrEmpty()) return saved
            val envKey: String? = runCatching<String?> { com.example.BuildConfig.ANGEL_ONE_API_KEY as String? }.getOrNull()
            if (!envKey.isNullOrBlank() && envKey != "ANGEL_ONE_API_KEY_DEFAULT_VALUE") return envKey ?: ""
            return ""
        }
        set(value) {
            safeSetToken(KEY_ANGEL_API_KEY, value)
        }

    var dhanAccessToken: String?
        get() = safeGetToken(KEY_DHAN_TOKEN)
        set(value) {
            safeSetToken(KEY_DHAN_TOKEN, value)
        }

    var dhanClientId: String
        get() = prefs.getString(KEY_DHAN_CLIENT_ID, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_DHAN_CLIENT_ID, value).commit()
        }

    var dhanTokenTimestamp: Long
        get() = prefs.getLong(KEY_DHAN_TOKEN_TIME, 0L)
        set(value) {
            prefs.edit().putLong(KEY_DHAN_TOKEN_TIME, value).commit()
        }

    var telegramBotToken: String
        get() = prefs.getString(KEY_TELEGRAM_BOT_TOKEN, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_TELEGRAM_BOT_TOKEN, value).commit()
        }

    var telegramChatId: String
        get() = prefs.getString(KEY_TELEGRAM_CHAT_ID, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_TELEGRAM_CHAT_ID, value).commit()
        }

    var isTelegramAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TELEGRAM_ALERTS_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_TELEGRAM_ALERTS_ENABLED, value).commit()
        }

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
        prefs.edit().putString("recent_searches_list", trimmed.joinToString("|||")).commit()
    }

    fun clearRecentSearches() {
        prefs.edit().remove("recent_searches_list").commit()
    }

    fun hasValidSession(): Boolean {
        val broker = activeBroker
        return when (broker) {
            "Angel One" -> !angelJwtToken.isNullOrBlank()
            "Dhan" -> !dhanAccessToken.isNullOrBlank()
            "m.Stock" -> isMStockConfigured()
            else -> {
                if (!dhanAccessToken.isNullOrBlank()) {
                    activeBroker = "Dhan"
                    true
                } else if (!angelJwtToken.isNullOrBlank()) {
                    activeBroker = "Angel One"
                    true
                } else if (isMStockConfigured()) {
                    activeBroker = "m.Stock"
                    true
                } else {
                    false
                }
            }
        }
    }

    fun clearDhanSession() {
        prefs.edit()
            .remove(KEY_DHAN_TOKEN)
            .remove(KEY_DHAN_CLIENT_ID)
            .remove(KEY_DHAN_TOKEN_TIME)
            .commit()
        if (activeBroker == "Dhan") {
            activeBroker = ""
        }
    }

    fun clearAngelSession() {
        prefs.edit()
            .remove(KEY_ANGEL_JWT)
            .remove(KEY_ANGEL_REFRESH)
            .remove(KEY_ANGEL_FEED)
            .remove(KEY_ANGEL_CLIENT_ID)
            .remove(KEY_ANGEL_TOKEN_TIME)
            .commit()
        if (activeBroker == "Angel One") {
            activeBroker = ""
        }
    }

    fun clearActiveBrokerSession() {
        when (activeBroker) {
            "Dhan" -> clearDhanSession()
            "Angel One" -> clearAngelSession()
            "m.Stock" -> clearMStockSession()
            else -> clearSession()
        }
    }

    var mstockApiKey: String
        get() = safeGetToken("mstock_api_key_enc") ?: ""
        set(value) {
            safeSetToken("mstock_api_key_enc", value)
        }

    var mstockClientId: String
        get() = prefs.getString("mstock_client_id", "") ?: ""
        set(value) {
            prefs.edit().putString("mstock_client_id", value).commit()
        }

    var mstockPasswordPin: String
        get() = safeGetToken("mstock_password_pin_enc") ?: ""
        set(value) {
            safeSetToken("mstock_password_pin_enc", value)
        }

    var mstockAccessToken: String?
        get() = safeGetToken("mstock_access_token_enc")
        set(value) {
            safeSetToken("mstock_access_token_enc", value)
        }

    var mstockTokenTimestamp: Long
        get() = prefs.getLong("mstock_token_time", 0L)
        set(value) {
            prefs.edit().putLong("mstock_token_time", value).commit()
        }

    fun isMStockConfigured(): Boolean {
        return !mstockApiKey.isBlank() || !mstockAccessToken.isNullOrBlank() || !mstockClientId.isBlank()
    }

    fun clearMStockSession() {
        prefs.edit()
            .remove("mstock_api_key_enc")
            .remove("mstock_client_id")
            .remove("mstock_access_token_enc")
            .remove("mstock_token_time")
            .commit()
        if (activeBroker == "m.Stock") {
            activeBroker = ""
        }
    }

    fun isTradeSmartConfigured(): Boolean {
        return !prefs.getString("tradesmart_api_key", "").isNullOrBlank()
    }

    fun clearSession() {
        prefs.edit().clear().commit()
    }
}
