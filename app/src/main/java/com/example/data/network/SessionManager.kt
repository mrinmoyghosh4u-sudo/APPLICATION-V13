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
            }.getOrElse { err ->
                Log.e("SessionManager", "EncryptedSharedPreferences recreation failed: ${err.message}")
                throw SecurityException("Failed to initialize secure encrypted storage: ${err.message}")
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
        private const val KEY_ANGEL_TOTP_SECRET = "angel_totp_secret_enc"
        private const val KEY_ANGEL_TOKEN_TIME = "angel_token_time"
        private const val KEY_DHAN_TOKEN = "dhan_access_token_enc"
        private const val KEY_DHAN_CLIENT_ID = "dhan_client_id"
        private const val KEY_DHAN_TOKEN_TIME = "dhan_token_time"
        private const val KEY_TELEGRAM_BOT_TOKEN = "telegram_bot_token"
        private const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"
        private const val KEY_TELEGRAM_CHANNEL_ID = "telegram_channel_id"
        private const val KEY_TELEGRAM_ALERTS_ENABLED = "telegram_alerts_enabled"
    }

    private fun safeGetToken(key: String): String? {
        val raw = prefs.getString(key, null)
        if (raw.isNullOrBlank()) return null
        return raw
    }

    private fun safeSetToken(key: String, value: String?) {
        if (value.isNullOrBlank()) {
            prefs.edit().remove(key).commit()
        } else {
            prefs.edit().putString(key, value.trim()).commit()
        }
    }

    // ==========================================
    // EXPLICIT BROKER CREDENTIAL STORE API
    // ==========================================

    fun saveAngelOneCredentials(
        clientCode: String,
        mpin: String = "",
        apiKey: String = "",
        totpSecret: String = ""
    ) {
        if (clientCode.isNotBlank()) angelClientId = clientCode.trim()
        if (mpin.isNotBlank()) angelMpin = mpin.trim()
        if (apiKey.isNotBlank()) angelApiKey = apiKey.trim()
        if (totpSecret.isNotBlank()) angelTotpSecret = totpSecret.trim()
    }

    fun clearAngelOneCredentials() {
        prefs.edit()
            .remove(KEY_ANGEL_CLIENT_ID)
            .remove(KEY_ANGEL_API_KEY)
            .remove("angel_mpin_enc")
            .remove(KEY_ANGEL_TOTP_SECRET)
            .remove(KEY_ANGEL_JWT)
            .remove(KEY_ANGEL_REFRESH)
            .remove(KEY_ANGEL_FEED)
            .remove(KEY_ANGEL_TOKEN_TIME)
            .putBoolean("is_angel_connected", false)
            .commit()
        if (activeBroker == "Angel One") {
            activeBroker = ""
        }
    }

    fun clearAngelSessionTokens() {
        prefs.edit()
            .remove(KEY_ANGEL_JWT)
            .remove(KEY_ANGEL_REFRESH)
            .remove(KEY_ANGEL_FEED)
            .remove(KEY_ANGEL_TOKEN_TIME)
            .putBoolean("is_angel_connected", false)
            .commit()
    }

    fun saveMStockCredentials(
        clientCode: String,
        apiKey: String = "",
        totpSecret: String = ""
    ) {
        if (clientCode.isNotBlank()) mstockClientId = clientCode.trim()
        if (apiKey.isNotBlank()) mstockApiKey = apiKey.trim()
        if (totpSecret.isNotBlank()) mstockTotpSecret = totpSecret.trim()
    }

    fun clearMStockCredentials() {
        prefs.edit()
            .remove("mstock_client_id")
            .remove("mstock_api_key_enc")
            .remove("mstock_totp_secret_enc")
            .remove("mstock_password_pin_enc")
            .remove("mstock_access_token_enc")
            .remove("mstock_refresh_token_enc")
            .remove("mstock_feed_token_enc")
            .remove("mstock_token_time")
            .commit()
        if (activeBroker == "m.Stock") {
            activeBroker = ""
        }
    }

    fun clearMStockSessionTokens() {
        prefs.edit()
            .remove("mstock_access_token_enc")
            .remove("mstock_refresh_token_enc")
            .remove("mstock_feed_token_enc")
            .remove("mstock_token_time")
            .commit()
    }

    fun clearDhanCredentials() {
        prefs.edit()
            .remove(KEY_DHAN_TOKEN)
            .remove(KEY_DHAN_CLIENT_ID)
            .remove(KEY_DHAN_TOKEN_TIME)
            .putBoolean("is_dhan_connected", false)
            .commit()
        if (activeBroker == "Dhan") {
            activeBroker = ""
        }
    }

    
    var activeMarketDataProvider: String
        get() = prefs.getString("active_market_provider", "Upstox") ?: "Upstox"
        set(value) = prefs.edit().putString("active_market_provider", value).apply()
        
    var activeOrderExecutionBroker: String = "Dhan"


    var activeBroker: String
        get() = "Dhan" // Always Dhan for execution
        set(value) {
            // prefs.edit().putString(KEY_ACTIVE_BROKER, "Dhan").commit()
        }

    var primaryMarketDataProvider: String
        get() = prefs.getString("primary_market_data_provider", "Angel One") ?: "Angel One"
        set(value) {
            prefs.edit().putString("primary_market_data_provider", value).commit()
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

    var angelTotpSecret: String
        get() = safeGetToken(KEY_ANGEL_TOTP_SECRET) ?: ""
        set(value) {
            safeSetToken(KEY_ANGEL_TOTP_SECRET, value)
        }

    fun isAngelConfigured(): Boolean {
        return !angelClientId.isBlank() && (!angelApiKey.isBlank() || !angelTotpSecret.isBlank() || !angelJwtToken.isNullOrBlank())
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

    var telegramChannelId: String
        get() = prefs.getString(KEY_TELEGRAM_CHANNEL_ID, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_TELEGRAM_CHANNEL_ID, value).commit()
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

    var isDhanConnected: Boolean
        get() = prefs.getBoolean("is_dhan_connected", false) && !dhanAccessToken.isNullOrBlank()
        set(value) {
            prefs.edit().putBoolean("is_dhan_connected", value).commit()
        }

    var isAngelConnected: Boolean
        get() = prefs.getBoolean("is_angel_connected", false) && !angelJwtToken.isNullOrBlank()
        set(value) {
            prefs.edit().putBoolean("is_angel_connected", value).commit()
        }

    fun hasAngelSession(): Boolean = !angelJwtToken.isNullOrBlank()
    fun hasDhanSession(): Boolean = !dhanAccessToken.isNullOrBlank()
    fun hasUpstoxSession(): Boolean = !upstoxAccessToken.isNullOrBlank()
    fun hasFyersSession(): Boolean = !fyersAccessToken.isNullOrBlank()
    fun hasMStockSession(): Boolean = isMStockConfigured()

    fun clearDhanSession() {
        prefs.edit()
            .remove(KEY_DHAN_TOKEN)
            .remove(KEY_DHAN_CLIENT_ID)
            .remove(KEY_DHAN_TOKEN_TIME)
            .putBoolean("is_dhan_connected", false)
            .commit()
        if (activeBroker == "Dhan") {
            activeBroker = ""
        }
    }

    fun clearAngelSession() {
        clearAngelSessionTokens()
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

    var mstockTotpSecret: String
        get() = safeGetToken("mstock_totp_secret_enc") ?: ""
        set(value) {
            safeSetToken("mstock_totp_secret_enc", value)
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

    var mstockRefreshToken: String?
        get() = safeGetToken("mstock_refresh_token_enc")
        set(value) {
            safeSetToken("mstock_refresh_token_enc", value)
        }

    var mstockFeedToken: String?
        get() = safeGetToken("mstock_feed_token_enc")
        set(value) {
            safeSetToken("mstock_feed_token_enc", value)
        }

    var mstockTokenTimestamp: Long
        get() = prefs.getLong("mstock_token_time", 0L)
        set(value) {
            prefs.edit().putLong("mstock_token_time", value).commit()
        }

    fun isMStockConfigured(): Boolean {
        return (!mstockClientId.isBlank() && (!mstockApiKey.isBlank() || !mstockTotpSecret.isBlank())) || !mstockAccessToken.isNullOrBlank()
    }

    fun clearMStockSession() {
        clearMStockSessionTokens()
    }

    fun clearSession() {
        prefs.edit().clear().commit()
    }

    // FYERS
    var fyersAppId: String
        get() = safeGetToken("fyers_app_id_enc") ?: ""
        set(value) {
            safeSetToken("fyers_app_id_enc", value)
            
        }

    var fyersSecretId: String
        get() = safeGetToken("fyers_secret_id_enc") ?: ""
        set(value) {
            safeSetToken("fyers_secret_id_enc", value)
            
        }

    
    var fyersPin: String
        get() = safeGetToken("fyers_pin_enc") ?: ""
        set(value) = safeSetToken("fyers_pin_enc", value)

    var fyersAccessToken: String?
        get() = safeGetToken("fyers_access_token_enc")
        set(value) = safeSetToken("fyers_access_token_enc", value)

    var fyersRefreshToken: String?
        get() = safeGetToken("fyers_refresh_token_enc")
        set(value) = safeSetToken("fyers_refresh_token_enc", value)
        
    var fyersTokenTimestamp: Long
        get() = prefs.getLong("fyers_token_time", 0L)
        set(value) = prefs.edit().putLong("fyers_token_time", value).apply()

    var fyersRedirectUri: String
        get() = prefs.getString("fyers_redirect_uri", com.example.util.FyersAuthHelper.DEFAULT_REDIRECT_URI) ?: com.example.util.FyersAuthHelper.DEFAULT_REDIRECT_URI
        set(value) = prefs.edit().putString("fyers_redirect_uri", value).apply()

    var isFyersConnected: Boolean
        get() = prefs.getBoolean("is_fyers_connected", false) && !fyersAccessToken.isNullOrBlank()
        set(value) = prefs.edit().putBoolean("is_fyers_connected", value).apply()

    fun isFyersConfigured(): Boolean {
        return !fyersAppId.isBlank() && !fyersAccessToken.isNullOrBlank()
    }

    fun clearFyersSession() {
        prefs.edit().apply {
            remove("fyers_token_time")
            putBoolean("is_fyers_connected", false)
        }.apply()
        safeSetToken("fyers_access_token_enc", null)
        safeSetToken("fyers_refresh_token_enc", null)
    }

    // ==========================================
    // UPSTOX (PRIMARY MARKET DATA)
    // ==========================================
    var upstoxApiKey: String
        get() = safeGetToken("upstox_api_key_enc") ?: ""
        set(value) {
            safeSetToken("upstox_api_key_enc", value)
        }

    var upstoxApiSecret: String
        get() = safeGetToken("upstox_api_secret_enc") ?: ""
        set(value) {
            safeSetToken("upstox_api_secret_enc", value)
        }

    var upstoxRedirectUri: String
        get() = prefs.getString("upstox_redirect_uri", "https://application-beige-psi.vercel.app/oauth") ?: "https://application-beige-psi.vercel.app/oauth"
        set(value) {
            prefs.edit().putString("upstox_redirect_uri", value).commit()
        }

    var upstoxAccessToken: String?
        get() = safeGetToken("upstox_access_token_enc")
        set(value) = safeSetToken("upstox_access_token_enc", value)

    var upstoxRefreshToken: String?
        get() = safeGetToken("upstox_refresh_token_enc")
        set(value) = safeSetToken("upstox_refresh_token_enc", value)

    var upstoxTokenTimestamp: Long
        get() = prefs.getLong("upstox_token_time", 0L)
        set(value) = prefs.edit().putLong("upstox_token_time", value).apply()

    var isUpstoxConnected: Boolean
        get() = prefs.getBoolean("is_upstox_connected", false) && !upstoxAccessToken.isNullOrBlank()
        set(value) = prefs.edit().putBoolean("is_upstox_connected", value).apply()

    fun isUpstoxConfigured(): Boolean {
        return !upstoxApiKey.isBlank() && !upstoxAccessToken.isNullOrBlank()
    }

    fun saveUpstoxCredentials(apiKey: String, apiSecret: String = "", redirectUri: String = "https://application-beige-psi.vercel.app/oauth") {
        if (apiKey.isNotBlank()) upstoxApiKey = apiKey.trim()
        if (apiSecret.isNotBlank()) upstoxApiSecret = apiSecret.trim()
        if (redirectUri.isNotBlank()) upstoxRedirectUri = redirectUri.trim()
    }

    fun clearUpstoxCredentials() {
        prefs.edit().apply {
            remove("upstox_token_time")
            remove("upstox_redirect_uri")
            putBoolean("is_upstox_connected", false)
        }.commit()
        safeSetToken("upstox_api_key_enc", null)
        safeSetToken("upstox_api_secret_enc", null)
        safeSetToken("upstox_access_token_enc", null)
        safeSetToken("upstox_refresh_token_enc", null)
    }

    fun clearUpstoxSession() {
        prefs.edit().apply {
            remove("upstox_token_time")
            putBoolean("is_upstox_connected", false)
        }.apply()
        safeSetToken("upstox_access_token_enc", null)
        safeSetToken("upstox_refresh_token_enc", null)
    }
}
