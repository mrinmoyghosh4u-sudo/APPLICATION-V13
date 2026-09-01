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
        } catch (e: Throwable) {
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
                Log.e("SessionManager", "EncryptedSharedPreferences recreation failed: ${err.message}, falling back to standard SharedPreferences")
                context.getSharedPreferences(prefFileName, Context.MODE_PRIVATE)
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
        if (mpin.isNotBlank()) angelClientPin = mpin.trim()
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



    var dhanClientId: String
        get() = prefs.getString("dhan_client_id", "") ?: ""
        set(value) = prefs.edit().putString("dhan_client_id", value).apply()

    var dhanAccessToken: String
        get() = prefs.getString("dhan_access_token", "") ?: ""
        set(value) = prefs.edit().putString("dhan_access_token", value).apply()

    var activeBroker: String
        get() = prefs.getString("active_broker", "") ?: ""
        set(value) = prefs.edit().putString("active_broker", value).apply()

    var pendingOAuthBroker: String
        get() = prefs.getString("pending_oauth_broker", "") ?: ""
        set(value) = prefs.edit().putString("pending_oauth_broker", value).apply()

    var isDhanConnected: Boolean
        get() = prefs.getBoolean("is_dhan_connected", false)
        set(value) = prefs.edit().putBoolean("is_dhan_connected", value).apply()

    var dhanTokenTimestamp: Long
        get() = prefs.getLong("dhan_token_timestamp", 0L)
        set(value) = prefs.edit().putLong("dhan_token_timestamp", value).apply()

    var lastCompletedDhanFingerprint: String
        get() = prefs.getString("last_completed_dhan_fingerprint", "") ?: ""
        set(value) = prefs.edit().putString("last_completed_dhan_fingerprint", value).apply()
        
    var dhanApiKey: String
        get() = prefs.getString("dhan_api_key", "") ?: ""
        set(value) = prefs.edit().putString("dhan_api_key", value).apply()
        
    var dhanClientSecret: String
        get() = prefs.getString("dhan_client_secret", "") ?: ""
        set(value) = prefs.edit().putString("dhan_client_secret", value).apply()
        
    fun clearActiveBrokerSession() {
        prefs.edit().remove("active_broker").apply()
    }

    var telegramBotToken: String
        get() = prefs.getString("telegram_bot_token", "") ?: ""
        set(value) = prefs.edit().putString("telegram_bot_token", value).apply()

    var telegramChatId: String
        get() = prefs.getString("telegram_chat_id", "") ?: ""
        set(value) = prefs.edit().putString("telegram_chat_id", value).apply()
        
    var telegramChannelId: String
        get() = prefs.getString("telegram_channel_id", "") ?: ""
        set(value) = prefs.edit().putString("telegram_channel_id", value).apply()
        
    var isTelegramAlertsEnabled: Boolean
        get() = prefs.getBoolean("telegram_alerts_enabled", false)
        set(value) = prefs.edit().putBoolean("telegram_alerts_enabled", value).apply()
        
    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean("is_biometric_enabled", false)
        set(value) = prefs.edit().putBoolean("is_biometric_enabled", value).apply()

    
    var isUpstoxConnected: Boolean
        get() = prefs.getBoolean("is_upstox_connected", false)
        set(value) = prefs.edit().putBoolean("is_upstox_connected", value).apply()

    var isFyersConnected: Boolean
        get() = prefs.getBoolean("is_fyers_connected", false)
        set(value) = prefs.edit().putBoolean("is_fyers_connected", value).apply()

    var isAngelConnected: Boolean
        get() = prefs.getBoolean("is_angel_connected", false)
        set(value) = prefs.edit().putBoolean("is_angel_connected", value).apply()

    var fyersAccessToken: String?
        get() = safeGetToken("fyers_access_token")
        set(value) = safeSetToken("fyers_access_token", value)

    var upstoxAccessToken: String?
        get() = safeGetToken("upstox_access_token")
        set(value) = safeSetToken("upstox_access_token", value)
fun hasValidSession(): Boolean {
        return isDhanConnected || isAngelConnected || isUpstoxConnected || isFyersConnected
    }

    fun markDhanTokenIdConsumed(token: String) {
        prefs.edit().putBoolean("dhan_token_consumed_$token", true).apply()
    }

    var fyersAppId: String
        get() = prefs.getString("fyers_app_id", "") ?: ""
        set(value) = prefs.edit().putString("fyers_app_id", value).apply()

    var fyersSecretId: String
        get() = prefs.getString("fyers_secret_id", "") ?: ""
        set(value) = prefs.edit().putString("fyers_secret_id", value).apply()

    var fyersRedirectUri: String
        get() = prefs.getString("fyers_redirect_uri", "kingkhan://oauth/callback") ?: "kingkhan://oauth/callback"
        set(value) = prefs.edit().putString("fyers_redirect_uri", value).apply()
        
    var upstoxApiKey: String
        get() = prefs.getString("upstox_api_key", "") ?: ""
        set(value) = prefs.edit().putString("upstox_api_key", value).apply()

    var upstoxApiSecret: String
        get() = prefs.getString("upstox_api_secret", "") ?: ""
        set(value) = prefs.edit().putString("upstox_api_secret", value).apply()

    fun isDhanTokenIdConsumed(token: String): Boolean {
        return prefs.getBoolean("dhan_token_consumed_$token", false)
    }

    data class PendingOAuthSession(
        val provider: String = "",
        val state: String = "",
        val createdAt: Long = 0L,
        val redirectUri: String = "",
        val consumed: Boolean = false
    )
    
    var pendingOAuthSession: PendingOAuthSession? = null
    var pendingUpstoxOAuthState: String = ""
    var pendingFyersOAuthState: String = ""
    var pendingOAuthState: String = ""
    var lastReceivedOAuthCode: String? = null
    var lastReceivedOAuthTime: Long = 0L

    fun hasUpstoxSession(): Boolean = isUpstoxConnected
    fun hasFyersSession(): Boolean = isFyersConnected
    fun hasAngelSession(): Boolean = isAngelConnected
    
    var angelTokenTimestamp: Long
        get() = prefs.getLong("angel_token_timestamp", 0L)
        set(value) = prefs.edit().putLong("angel_token_timestamp", value).apply()
        
    var angelClientId: String
        get() = prefs.getString("angel_client_id", "") ?: ""
        set(value) = prefs.edit().putString("angel_client_id", value).apply()


    var angelJwtToken: String
        get() = prefs.getString(KEY_ANGEL_JWT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANGEL_JWT, value).apply()

    var angelAuthToken: String
        get() = prefs.getString(KEY_ANGEL_JWT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANGEL_JWT, value).apply()
        
    var angelFeedToken: String
        get() = prefs.getString(KEY_ANGEL_FEED, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANGEL_FEED, value).apply()
        
    var angelRefreshToken: String
        get() = prefs.getString(KEY_ANGEL_REFRESH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANGEL_REFRESH, value).apply()

    var angelApiKey: String
        get() = prefs.getString(KEY_ANGEL_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANGEL_API_KEY, value).apply()
        
    var angelClientCode: String
        get() = prefs.getString(KEY_ANGEL_CLIENT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANGEL_CLIENT_ID, value).apply()

    var angelClientPin: String
        get() = prefs.getString("angel_mpin_enc", "") ?: ""
        set(value) = prefs.edit().putString("angel_mpin_enc", value).apply()

    var angelTotpSecret: String
        get() = prefs.getString(KEY_ANGEL_TOTP_SECRET, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANGEL_TOTP_SECRET, value).apply()

    var upstoxRefreshToken: String? = null
    var upstoxTokenTimestamp: Long = 0L

    fun clearUpstoxSession() {
        upstoxAccessToken = null
        upstoxRefreshToken = null
        isUpstoxConnected = false
        upstoxTokenTimestamp = 0L
    }

    var lastProcessedOAuthCode: String? = null
    var lastProcessedOAuthTime: Long = 0L
    var fyersRefreshToken: String? = null
    var fyersTokenTimestamp: Long = 0L
    var fyersPin: String? = null

    fun clearFyersSession() {
        fyersAccessToken = null
        fyersRefreshToken = null
        isFyersConnected = false
        fyersTokenTimestamp = 0L
    }

    fun saveUpstoxCredentials(apiKey: String, apiSecret: String, token: String = "") {
        upstoxApiKey = apiKey
        upstoxApiSecret = apiSecret
        if (token.isNotEmpty()) {
            upstoxAccessToken = token
            isUpstoxConnected = true
        }
    }

    fun saveFyersCredentials(appId: String, secretId: String, token: String = "") {
        fyersAppId = appId
        fyersSecretId = secretId
        if (token.isNotEmpty()) {
            fyersAccessToken = token
            isFyersConnected = true
        }
    }

}
