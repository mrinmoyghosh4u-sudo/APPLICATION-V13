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
        val sharedPrefs = try {
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
        migrateLegacyKeys(sharedPrefs)
        return sharedPrefs
    }

    companion object {
        // Active broker & system keys
        private const val KEY_ACTIVE_BROKER = "active_broker"
        private const val KEY_BIOMETRIC_ENABLED = "is_biometric_enabled"

        // Angel One Canonical Keys
        private const val KEY_ANGEL_JWT = "angel_jwt_token_enc"
        private const val KEY_ANGEL_REFRESH = "angel_refresh_token_enc"
        private const val KEY_ANGEL_FEED = "angel_feed_token_enc"
        private const val KEY_ANGEL_CLIENT_ID = "angel_client_id"
        private const val KEY_ANGEL_API_KEY = "angel_api_key_enc"
        private const val KEY_ANGEL_TOTP_SECRET = "angel_totp_secret_enc"
        private const val KEY_ANGEL_MPIN = "angel_mpin_enc"
        private const val KEY_ANGEL_TOKEN_TIMESTAMP = "angel_token_timestamp"
        private const val KEY_IS_ANGEL_CONNECTED = "is_angel_connected"

        // Dhan Canonical Keys
        private const val KEY_DHAN_TOKEN = "dhan_access_token_enc"
        private const val KEY_DHAN_CLIENT_ID = "dhan_client_id"
        private const val KEY_DHAN_API_KEY = "dhan_api_key"
        private const val KEY_DHAN_CLIENT_SECRET = "dhan_client_secret"
        private const val KEY_DHAN_TOKEN_TIMESTAMP = "dhan_token_timestamp"
        private const val KEY_IS_DHAN_CONNECTED = "is_dhan_connected"
        private const val KEY_LAST_COMPLETED_DHAN_FINGERPRINT = "last_completed_dhan_fingerprint"

        // Upstox Canonical Keys
        private const val KEY_UPSTOX_TOKEN = "upstox_access_token_enc"
        private const val KEY_UPSTOX_REFRESH_TOKEN = "upstox_refresh_token_enc"
        private const val KEY_UPSTOX_TOKEN_TIMESTAMP = "upstox_token_timestamp"
        private const val KEY_UPSTOX_API_KEY = "upstox_api_key"
        private const val KEY_UPSTOX_API_SECRET = "upstox_api_secret"
        private const val KEY_IS_UPSTOX_CONNECTED = "is_upstox_connected"

        // Fyers Canonical Keys
        private const val KEY_FYERS_TOKEN = "fyers_access_token_enc"
        private const val KEY_FYERS_REFRESH_TOKEN = "fyers_refresh_token_enc"
        private const val KEY_FYERS_TOKEN_TIMESTAMP = "fyers_token_timestamp"
        private const val KEY_FYERS_APP_ID = "fyers_app_id"
        private const val KEY_FYERS_SECRET_ID = "fyers_secret_id"
        private const val KEY_FYERS_PIN = "fyers_pin_enc"
        private const val KEY_FYERS_REDIRECT_URI = "fyers_redirect_uri"
        private const val KEY_IS_FYERS_CONNECTED = "is_fyers_connected"

        // Persistent OAuth State Keys
        private const val KEY_PENDING_OAUTH_PROVIDER = "pending_oauth_provider"
        private const val KEY_PENDING_OAUTH_STATE = "pending_oauth_state"
        private const val KEY_PENDING_OAUTH_CREATED_AT = "pending_oauth_created_at"
        private const val KEY_PENDING_OAUTH_REDIRECT_URI = "pending_oauth_redirect_uri"
        private const val KEY_PENDING_OAUTH_CONSUMED = "pending_oauth_consumed"
        private const val KEY_LAST_PROCESSED_OAUTH_CODE = "last_processed_oauth_code"
        private const val KEY_LAST_PROCESSED_OAUTH_TIME = "last_processed_oauth_time"

        // Telegram Keys
        private const val KEY_TELEGRAM_BOT_TOKEN = "telegram_bot_token"
        private const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"
        private const val KEY_TELEGRAM_CHANNEL_ID = "telegram_channel_id"
        private const val KEY_TELEGRAM_ALERTS_ENABLED = "telegram_alerts_enabled"
    }

    private fun migrateLegacyKeys(preferences: SharedPreferences) {
        val editor = preferences.edit()
        var modified = false

        // 1. Angel One timestamp unification: migrate "angel_token_time" -> "angel_token_timestamp"
        if (preferences.contains("angel_token_time")) {
            val oldTime = preferences.getLong("angel_token_time", 0L)
            if (oldTime > 0L && preferences.getLong(KEY_ANGEL_TOKEN_TIMESTAMP, 0L) == 0L) {
                editor.putLong(KEY_ANGEL_TOKEN_TIMESTAMP, oldTime)
            }
            editor.remove("angel_token_time")
            modified = true
        }

        // 2. Dhan token unification: migrate "dhan_access_token" -> "dhan_access_token_enc"
        if (preferences.contains("dhan_access_token")) {
            val oldToken = preferences.getString("dhan_access_token", null)
            if (!oldToken.isNullOrBlank() && preferences.getString(KEY_DHAN_TOKEN, null).isNullOrBlank()) {
                editor.putString(KEY_DHAN_TOKEN, oldToken)
            }
            editor.remove("dhan_access_token")
            modified = true
        }

        // 3. Dhan timestamp unification: migrate "dhan_token_time" -> "dhan_token_timestamp"
        if (preferences.contains("dhan_token_time")) {
            val oldTime = preferences.getLong("dhan_token_time", 0L)
            if (oldTime > 0L && preferences.getLong(KEY_DHAN_TOKEN_TIMESTAMP, 0L) == 0L) {
                editor.putLong(KEY_DHAN_TOKEN_TIMESTAMP, oldTime)
            }
            editor.remove("dhan_token_time")
            modified = true
        }

        // 4. Upstox token unification: migrate "upstox_access_token" -> "upstox_access_token_enc"
        if (preferences.contains("upstox_access_token")) {
            val oldToken = preferences.getString("upstox_access_token", null)
            if (!oldToken.isNullOrBlank() && preferences.getString(KEY_UPSTOX_TOKEN, null).isNullOrBlank()) {
                editor.putString(KEY_UPSTOX_TOKEN, oldToken)
            }
            editor.remove("upstox_access_token")
            modified = true
        }

        // 5. Fyers token unification: migrate "fyers_access_token" -> "fyers_access_token_enc"
        if (preferences.contains("fyers_access_token")) {
            val oldToken = preferences.getString("fyers_access_token", null)
            if (!oldToken.isNullOrBlank() && preferences.getString(KEY_FYERS_TOKEN, null).isNullOrBlank()) {
                editor.putString(KEY_FYERS_TOKEN, oldToken)
            }
            editor.remove("fyers_access_token")
            modified = true
        }

        if (modified) {
            editor.apply()
            Log.i("SessionManager", "[MIGRATION_COMPLETE] Legacy session keys safely migrated to canonical schema")
        }
    }

    private fun safeGetToken(key: String): String? {
        val raw = prefs.getString(key, null)
        if (raw.isNullOrBlank()) return null
        return raw
    }

    private fun safeSetToken(key: String, value: String?) {
        if (value.isNullOrBlank()) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, value.trim()).apply()
        }
    }

    // ==========================================
    // ACTIVE BROKER & SYSTEM
    // ==========================================

    var activeBroker: String
        get() = prefs.getString(KEY_ACTIVE_BROKER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACTIVE_BROKER, value.trim()).apply()

    fun clearActiveBrokerSession() {
        prefs.edit().remove(KEY_ACTIVE_BROKER).apply()
    }

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    fun hasValidSession(): Boolean {
        return (isDhanConnected && dhanAccessToken.isNotBlank()) ||
               (isAngelConnected && angelAuthToken.isNotBlank()) ||
               (isUpstoxConnected && !upstoxAccessToken.isNullOrBlank()) ||
               (isFyersConnected && !fyersAccessToken.isNullOrBlank())
    }

    // ==========================================
    // DHAN CREDENTIALS & SESSIONS
    // ==========================================

    var dhanClientId: String
        get() = prefs.getString(KEY_DHAN_CLIENT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DHAN_CLIENT_ID, value.trim()).apply()

    var dhanAccessToken: String
        get() = safeGetToken(KEY_DHAN_TOKEN) ?: ""
        set(value) = safeSetToken(KEY_DHAN_TOKEN, value)

    var dhanApiKey: String
        get() = prefs.getString(KEY_DHAN_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DHAN_API_KEY, value.trim()).apply()

    var dhanClientSecret: String
        get() = safeGetToken(KEY_DHAN_CLIENT_SECRET) ?: ""
        set(value) = safeSetToken(KEY_DHAN_CLIENT_SECRET, value)

    var isDhanConnected: Boolean
        get() = prefs.getBoolean(KEY_IS_DHAN_CONNECTED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_DHAN_CONNECTED, value).apply()

    var dhanTokenTimestamp: Long
        get() = prefs.getLong(KEY_DHAN_TOKEN_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_DHAN_TOKEN_TIMESTAMP, value).apply()

    var lastCompletedDhanFingerprint: String
        get() = prefs.getString(KEY_LAST_COMPLETED_DHAN_FINGERPRINT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_COMPLETED_DHAN_FINGERPRINT, value).apply()

    fun saveDhanCredentials(clientId: String, accessToken: String, apiKey: String = "", clientSecret: String = "") {
        if (clientId.isNotBlank()) dhanClientId = clientId.trim()
        if (accessToken.isNotBlank()) {
            dhanAccessToken = accessToken.trim()
            dhanTokenTimestamp = System.currentTimeMillis()
            isDhanConnected = true
        }
        if (apiKey.isNotBlank()) dhanApiKey = apiKey.trim()
        if (clientSecret.isNotBlank()) dhanClientSecret = clientSecret.trim()
    }

    fun clearDhanCredentials() {
        prefs.edit()
            .remove(KEY_DHAN_CLIENT_ID)
            .remove(KEY_DHAN_TOKEN)
            .remove(KEY_DHAN_API_KEY)
            .remove(KEY_DHAN_CLIENT_SECRET)
            .remove(KEY_DHAN_TOKEN_TIMESTAMP)
            .remove(KEY_LAST_COMPLETED_DHAN_FINGERPRINT)
            .putBoolean(KEY_IS_DHAN_CONNECTED, false)
            .apply()
        if (activeBroker == "Dhan") {
            activeBroker = ""
        }
    }

    fun markDhanTokenIdConsumed(token: String) {
        prefs.edit().putBoolean("dhan_token_consumed_$token", true).apply()
    }

    fun isDhanTokenIdConsumed(token: String): Boolean {
        return prefs.getBoolean("dhan_token_consumed_$token", false)
    }

    // ==========================================
    // ANGEL ONE CREDENTIALS & SESSIONS
    // ==========================================

    var angelClientId: String
        get() = prefs.getString(KEY_ANGEL_CLIENT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ANGEL_CLIENT_ID, value.trim()).apply()

    var angelClientCode: String
        get() = angelClientId
        set(value) { angelClientId = value }

    var angelApiKey: String
        get() = safeGetToken(KEY_ANGEL_API_KEY) ?: ""
        set(value) = safeSetToken(KEY_ANGEL_API_KEY, value)

    var angelClientPin: String
        get() = safeGetToken(KEY_ANGEL_MPIN) ?: ""
        set(value) = safeSetToken(KEY_ANGEL_MPIN, value)

    var angelTotpSecret: String
        get() = safeGetToken(KEY_ANGEL_TOTP_SECRET) ?: ""
        set(value) = safeSetToken(KEY_ANGEL_TOTP_SECRET, value)

    var angelAuthToken: String
        get() = safeGetToken(KEY_ANGEL_JWT) ?: ""
        set(value) = safeSetToken(KEY_ANGEL_JWT, value)

    var angelJwtToken: String
        get() = angelAuthToken
        set(value) { angelAuthToken = value }

    var angelRefreshToken: String
        get() = safeGetToken(KEY_ANGEL_REFRESH) ?: ""
        set(value) = safeSetToken(KEY_ANGEL_REFRESH, value)

    var angelFeedToken: String
        get() = safeGetToken(KEY_ANGEL_FEED) ?: ""
        set(value) = safeSetToken(KEY_ANGEL_FEED, value)

    var angelTokenTimestamp: Long
        get() = prefs.getLong(KEY_ANGEL_TOKEN_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_ANGEL_TOKEN_TIMESTAMP, value).apply()

    var isAngelConnected: Boolean
        get() = prefs.getBoolean(KEY_IS_ANGEL_CONNECTED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ANGEL_CONNECTED, value).apply()

    fun hasAngelSession(): Boolean = isAngelConnected && angelAuthToken.isNotBlank()

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
            .remove(KEY_ANGEL_MPIN)
            .remove(KEY_ANGEL_TOTP_SECRET)
            .remove(KEY_ANGEL_JWT)
            .remove(KEY_ANGEL_REFRESH)
            .remove(KEY_ANGEL_FEED)
            .remove(KEY_ANGEL_TOKEN_TIMESTAMP)
            .putBoolean(KEY_IS_ANGEL_CONNECTED, false)
            .apply()
        if (activeBroker == "Angel One") {
            activeBroker = ""
        }
    }

    fun clearAngelSessionTokens() {
        prefs.edit()
            .remove(KEY_ANGEL_JWT)
            .remove(KEY_ANGEL_REFRESH)
            .remove(KEY_ANGEL_FEED)
            .remove(KEY_ANGEL_TOKEN_TIMESTAMP)
            .putBoolean(KEY_IS_ANGEL_CONNECTED, false)
            .apply()
    }

    // ==========================================
    // UPSTOX CREDENTIALS & SESSIONS
    // ==========================================

    var upstoxApiKey: String
        get() = prefs.getString(KEY_UPSTOX_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_UPSTOX_API_KEY, value.trim()).apply()

    var upstoxApiSecret: String
        get() = safeGetToken(KEY_UPSTOX_API_SECRET) ?: ""
        set(value) = safeSetToken(KEY_UPSTOX_API_SECRET, value)

    var upstoxAccessToken: String?
        get() = safeGetToken(KEY_UPSTOX_TOKEN)
        set(value) = safeSetToken(KEY_UPSTOX_TOKEN, value)

    var upstoxRefreshToken: String?
        get() = safeGetToken(KEY_UPSTOX_REFRESH_TOKEN)
        set(value) = safeSetToken(KEY_UPSTOX_REFRESH_TOKEN, value)

    var upstoxTokenTimestamp: Long
        get() = prefs.getLong(KEY_UPSTOX_TOKEN_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_UPSTOX_TOKEN_TIMESTAMP, value).apply()

    var isUpstoxConnected: Boolean
        get() = prefs.getBoolean(KEY_IS_UPSTOX_CONNECTED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_UPSTOX_CONNECTED, value).apply()

    fun hasUpstoxSession(): Boolean = isUpstoxConnected && !upstoxAccessToken.isNullOrBlank()

    fun saveUpstoxCredentials(apiKey: String, apiSecret: String, token: String = "") {
        upstoxApiKey = apiKey.trim()
        upstoxApiSecret = apiSecret.trim()
        if (token.isNotBlank()) {
            upstoxAccessToken = token.trim()
            upstoxTokenTimestamp = System.currentTimeMillis()
            isUpstoxConnected = true
        }
    }

    fun clearUpstoxSession() {
        prefs.edit()
            .remove(KEY_UPSTOX_TOKEN)
            .remove(KEY_UPSTOX_REFRESH_TOKEN)
            .remove(KEY_UPSTOX_TOKEN_TIMESTAMP)
            .putBoolean(KEY_IS_UPSTOX_CONNECTED, false)
            .apply()
        if (activeBroker == "Upstox") {
            activeBroker = ""
        }
    }

    // ==========================================
    // FYERS CREDENTIALS & SESSIONS
    // ==========================================

    var fyersAppId: String
        get() = prefs.getString(KEY_FYERS_APP_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FYERS_APP_ID, value.trim()).apply()

    var fyersSecretId: String
        get() = safeGetToken(KEY_FYERS_SECRET_ID) ?: ""
        set(value) = safeSetToken(KEY_FYERS_SECRET_ID, value)

    var fyersRedirectUri: String
        get() = prefs.getString(KEY_FYERS_REDIRECT_URI, "kingkhan://oauth/callback") ?: "kingkhan://oauth/callback"
        set(value) = prefs.edit().putString(KEY_FYERS_REDIRECT_URI, value.trim()).apply()

    var fyersAccessToken: String?
        get() = safeGetToken(KEY_FYERS_TOKEN)
        set(value) = safeSetToken(KEY_FYERS_TOKEN, value)

    var fyersRefreshToken: String?
        get() = safeGetToken(KEY_FYERS_REFRESH_TOKEN)
        set(value) = safeSetToken(KEY_FYERS_REFRESH_TOKEN, value)

    var fyersTokenTimestamp: Long
        get() = prefs.getLong(KEY_FYERS_TOKEN_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_FYERS_TOKEN_TIMESTAMP, value).apply()

    var fyersPin: String?
        get() = safeGetToken(KEY_FYERS_PIN)
        set(value) = safeSetToken(KEY_FYERS_PIN, value)

    var isFyersConnected: Boolean
        get() = prefs.getBoolean(KEY_IS_FYERS_CONNECTED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_FYERS_CONNECTED, value).apply()

    fun hasFyersSession(): Boolean = isFyersConnected && !fyersAccessToken.isNullOrBlank()

    fun saveFyersCredentials(appId: String, secretId: String, token: String = "") {
        fyersAppId = appId.trim()
        fyersSecretId = secretId.trim()
        if (token.isNotBlank()) {
            fyersAccessToken = token.trim()
            fyersTokenTimestamp = System.currentTimeMillis()
            isFyersConnected = true
        }
    }

    fun clearFyersSession() {
        prefs.edit()
            .remove(KEY_FYERS_TOKEN)
            .remove(KEY_FYERS_REFRESH_TOKEN)
            .remove(KEY_FYERS_TOKEN_TIMESTAMP)
            .remove(KEY_FYERS_PIN)
            .putBoolean(KEY_IS_FYERS_CONNECTED, false)
            .apply()
        if (activeBroker == "Fyers") {
            activeBroker = ""
        }
    }

    // ==========================================
    // SECURE OAUTH TRANSACTION PERSISTENCE
    // ==========================================

    data class PendingOAuthSession(
        val provider: String = "",
        val state: String = "",
        val createdAt: Long = 0L,
        val redirectUri: String = "",
        val consumed: Boolean = false
    )

    var pendingOAuthSession: PendingOAuthSession?
        get() {
            val provider = prefs.getString(KEY_PENDING_OAUTH_PROVIDER, null) ?: return null
            val state = prefs.getString(KEY_PENDING_OAUTH_STATE, "") ?: ""
            val createdAt = prefs.getLong(KEY_PENDING_OAUTH_CREATED_AT, 0L)
            val redirectUri = prefs.getString(KEY_PENDING_OAUTH_REDIRECT_URI, "") ?: ""
            val consumed = prefs.getBoolean(KEY_PENDING_OAUTH_CONSUMED, false)

            if (provider.isBlank() || createdAt == 0L) return null
            // Auto expire after 15 minutes
            if (System.currentTimeMillis() - createdAt > 15 * 60 * 1000L) {
                clearPendingOAuthSession()
                return null
            }
            return PendingOAuthSession(provider, state, createdAt, redirectUri, consumed)
        }
        set(value) {
            if (value == null) {
                clearPendingOAuthSession()
            } else {
                prefs.edit()
                    .putString(KEY_PENDING_OAUTH_PROVIDER, value.provider)
                    .putString(KEY_PENDING_OAUTH_STATE, value.state)
                    .putLong(KEY_PENDING_OAUTH_CREATED_AT, value.createdAt)
                    .putString(KEY_PENDING_OAUTH_REDIRECT_URI, value.redirectUri)
                    .putBoolean(KEY_PENDING_OAUTH_CONSUMED, value.consumed)
                    .apply()
            }
        }

    fun clearPendingOAuthSession() {
        prefs.edit()
            .remove(KEY_PENDING_OAUTH_PROVIDER)
            .remove(KEY_PENDING_OAUTH_STATE)
            .remove(KEY_PENDING_OAUTH_CREATED_AT)
            .remove(KEY_PENDING_OAUTH_REDIRECT_URI)
            .remove(KEY_PENDING_OAUTH_CONSUMED)
            .apply()
    }

    var pendingOAuthBroker: String
        get() = prefs.getString("pending_oauth_broker", "") ?: ""
        set(value) = prefs.edit().putString("pending_oauth_broker", value).apply()

    var pendingUpstoxOAuthState: String
        get() = prefs.getString("pending_upstox_oauth_state", "") ?: ""
        set(value) = prefs.edit().putString("pending_upstox_oauth_state", value).apply()

    var pendingFyersOAuthState: String
        get() = prefs.getString("pending_fyers_oauth_state", "") ?: ""
        set(value) = prefs.edit().putString("pending_fyers_oauth_state", value).apply()

    var pendingOAuthState: String
        get() = prefs.getString("pending_oauth_state_generic", "") ?: ""
        set(value) = prefs.edit().putString("pending_oauth_state_generic", value).apply()

    var lastReceivedOAuthCode: String?
        get() = prefs.getString("last_received_oauth_code", null)
        set(value) = if (value == null) prefs.edit().remove("last_received_oauth_code").apply() else prefs.edit().putString("last_received_oauth_code", value).apply()

    var lastReceivedOAuthTime: Long
        get() = prefs.getLong("last_received_oauth_time", 0L)
        set(value) = prefs.edit().putLong("last_received_oauth_time", value).apply()

    var lastProcessedOAuthCode: String?
        get() = prefs.getString(KEY_LAST_PROCESSED_OAUTH_CODE, null)
        set(value) = if (value == null) prefs.edit().remove(KEY_LAST_PROCESSED_OAUTH_CODE).apply() else prefs.edit().putString(KEY_LAST_PROCESSED_OAUTH_CODE, value).apply()

    var lastProcessedOAuthTime: Long
        get() = prefs.getLong(KEY_LAST_PROCESSED_OAUTH_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_PROCESSED_OAUTH_TIME, value).apply()

    // ==========================================
    // TELEGRAM SETTINGS
    // ==========================================

    var telegramBotToken: String
        get() = safeGetToken(KEY_TELEGRAM_BOT_TOKEN) ?: ""
        set(value) = safeSetToken(KEY_TELEGRAM_BOT_TOKEN, value)

    var telegramChatId: String
        get() = prefs.getString(KEY_TELEGRAM_CHAT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TELEGRAM_CHAT_ID, value.trim()).apply()

    var telegramChannelId: String
        get() = prefs.getString(KEY_TELEGRAM_CHANNEL_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TELEGRAM_CHANNEL_ID, value.trim()).apply()

    var isTelegramAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TELEGRAM_ALERTS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_TELEGRAM_ALERTS_ENABLED, value).apply()
}
