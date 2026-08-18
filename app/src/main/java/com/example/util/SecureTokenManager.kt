package com.example.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureTokenManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("secure_tokens_store", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALIAS = "king_khan_github_token_key"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        private const val PREF_KEY_ENCRYPTED_TOKEN = "enc_github_token"

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

    @Volatile
    private var softwareFallbackKey: SecretKey? = null

    private fun getSoftwareFallbackKey(): SecretKey {
        softwareFallbackKey?.let { return it }

        val cachedB64 = prefs.getString("sw_fallback_key", null)
        if (!cachedB64.isNullOrBlank()) {
            try {
                val keyBytes = Base64.decode(cachedB64, Base64.NO_WRAP)
                val secretKey = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
                softwareFallbackKey = secretKey
                return secretKey
            } catch (e: Exception) {
                Log.e("SecureTokenManager", "Error loading fallback key: ${e.message}")
            }
        }

        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val secretKey = keyGen.generateKey()
        val keyB64 = Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
        prefs.edit().putString("sw_fallback_key", keyB64).apply()
        softwareFallbackKey = secretKey
        return secretKey
    }

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                if (entry != null) {
                    return entry.secretKey
                }
            }

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
            val keySpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keySpec)
            keyGenerator.generateKey()
        } catch (t: Throwable) {
            Log.w("SecureTokenManager", "AndroidKeyStore unavailable (${t.message}), using fallback AES key")
            getSoftwareFallbackKey()
        }
    }

    fun saveGithubToken(token: String): Boolean {
        val trimmed = token.trim()
        if (trimmed.isBlank()) {
            clearGithubToken()
            return true
        }

        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))

            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

            prefs.edit().putString(PREF_KEY_ENCRYPTED_TOKEN, "$ivBase64:$encBase64").apply()
            AppPreferences.getInstance(appContext).clearLegacyGithubToken()
            true
        } catch (e: Exception) {
            Log.e("SecureTokenManager", "Error saving encrypted token: ${e.message}")
            false
        }
    }

    fun getGithubToken(): String {
        val payload = prefs.getString(PREF_KEY_ENCRYPTED_TOKEN, null)
        if (payload.isNullOrBlank()) {
            val legacyToken = AppPreferences.getInstance(appContext).getGithubTokenRaw()
            if (legacyToken.isNotBlank()) {
                saveGithubToken(legacyToken)
                return legacyToken
            }
            return ""
        }

        return try {
            val parts = payload.split(":")
            if (parts.size != 2) return ""

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("SecureTokenManager", "Error decrypting token: ${e.message}")
            ""
        }
    }

    fun clearGithubToken() {
        prefs.edit().remove(PREF_KEY_ENCRYPTED_TOKEN).apply()
        AppPreferences.getInstance(appContext).clearLegacyGithubToken()
    }

    fun hasGithubToken(): Boolean {
        return getGithubToken().isNotBlank()
    }

    fun getMaskedGithubToken(): String {
        val token = getGithubToken()
        return if (token.isNotBlank()) "●●●●●●●●" else "NOT SET"
    }
}
