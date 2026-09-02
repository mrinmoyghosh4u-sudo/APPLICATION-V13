package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.network.InMemorySharedPreferences

class SecureTokenManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private var isSecureStorageAvailable = true

    private val securePrefs: SharedPreferences by lazy {
        initSecurePrefs(appContext)
    }

    private fun initSecurePrefs(context: Context): SharedPreferences {
        val prefFileName = "kingkhan_secure_tokens_v2"
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
            Log.e("SecureTokenManager", "EncryptedSharedPreferences init failed: ${e.message}, attempting recreation")
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
                Log.e("SecureTokenManager", "Secure storage unavailable: ${err.message}. FAILING CLOSED. Plaintext fallback is strictly forbidden.")
                isSecureStorageAvailable = false
                InMemorySharedPreferences()
            }
        }
    }

    companion object {
        private const val PREF_KEY_TOKEN = "github_token"

        @Volatile
        private var INSTANCE: SecureTokenManager? = null

        fun getInstance(context: Context): SecureTokenManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SecureTokenManager(context)
                INSTANCE = instance
                instance
            }
        }
    }

    init {
        // Purge any legacy unencrypted fallback keys and tokens
        try {
            val legacyStore = appContext.getSharedPreferences("secure_tokens_store", Context.MODE_PRIVATE)
            if (legacyStore.all.isNotEmpty()) {
                legacyStore.edit().clear().apply()
            }
        } catch (_: Exception) {}
    }

    fun saveGithubToken(token: String): Boolean {
        val trimmed = token.trim()
        if (trimmed.isBlank()) {
            clearGithubToken()
            return true
        }

        return try {
            securePrefs.edit().putString(PREF_KEY_TOKEN, trimmed).apply()
            AppPreferences.getInstance(appContext).clearLegacyGithubToken()
            true
        } catch (e: Exception) {
            Log.e("SecureTokenManager", "Error saving encrypted token: ${e.message}")
            false
        }
    }

    fun getGithubToken(): String {
        return try {
            val token = securePrefs.getString(PREF_KEY_TOKEN, null)
            if (!token.isNullOrBlank()) {
                return token
            }
            val legacyToken = AppPreferences.getInstance(appContext).getGithubTokenRaw()
            if (legacyToken.isNotBlank()) {
                saveGithubToken(legacyToken)
                return legacyToken
            }
            ""
        } catch (e: Exception) {
            Log.e("SecureTokenManager", "Error retrieving token: ${e.message}")
            ""
        }
    }

    fun clearGithubToken() {
        try {
            securePrefs.edit().remove(PREF_KEY_TOKEN).apply()
            AppPreferences.getInstance(appContext).clearLegacyGithubToken()
        } catch (e: Exception) {
            Log.e("SecureTokenManager", "Error clearing token: ${e.message}")
        }
    }

    fun hasGithubToken(): Boolean {
        return getGithubToken().isNotBlank()
    }

    fun getMaskedGithubToken(): String {
        val token = getGithubToken()
        return if (token.isNotBlank()) "●●●●●●●●" else "NOT SET"
    }
}
