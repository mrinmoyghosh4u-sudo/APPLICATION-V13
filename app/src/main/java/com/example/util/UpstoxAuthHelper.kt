package com.example.util

import android.util.Log
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

object UpstoxAuthHelper {

    private const val TAG = "UpstoxAuthHelper"
    const val DEFAULT_REDIRECT_URI = "https://application-beige-psi.vercel.app/oauth"
    private const val AUTH_DIALOG_BASE = "https://api-v2.upstox.com/v2/login/authorization/dialog"
    private const val STATE_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes state validity window

    private data class OAuthStateEntry(
        val state: String,
        val createdAt: Long = System.currentTimeMillis(),
        var consumed: Boolean = false
    )

    // Thread-safe registry for single-use OAuth states
    private val activeStates = ConcurrentHashMap<String, OAuthStateEntry>()
    // Processed auth codes tracking to prevent code replay attacks
    private val consumedAuthCodes = ConcurrentHashMap.newKeySet<String>()

    /**
     * Generates a cryptographically secure OAuth state parameter, registers it with timestamp,
     * and returns the state string.
     */
    fun generateSecureState(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        val randomHex = bytes.joinToString("") { "%02x".format(it) }
        val state = "upstox_$randomHex"

        // Clean up expired states older than 10 minutes
        val now = System.currentTimeMillis()
        activeStates.entries.removeIf { now - it.value.createdAt > 10 * 60 * 1000L }

        activeStates[state] = OAuthStateEntry(state = state, createdAt = now)
        Log.i(TAG, "[UPSTOX_OAUTH_STATE_GENERATED] Secure state created: ${state.take(12)}...")
        return state
    }

    /**
     * Validates an incoming state parameter against registered active states.
     * Enforces single-use consumption and expiration rules.
     */
    fun validateAndConsumeState(incomingState: String?): Boolean {
        if (incomingState.isNullOrBlank()) {
            Log.w(TAG, "[UPSTOX_OAUTH_STATE_INVALID] State parameter is null or blank")
            return false
        }

        val entry = activeStates[incomingState]
        if (entry == null) {
            Log.w(TAG, "[UPSTOX_OAUTH_STATE_INVALID] Unrecognized or untracked OAuth state")
            return false
        }

        if (entry.consumed) {
            Log.w(TAG, "[UPSTOX_OAUTH_STATE_REPLAY] OAuth state has already been consumed")
            return false
        }

        val age = System.currentTimeMillis() - entry.createdAt
        if (age > STATE_EXPIRY_MS) {
            Log.w(TAG, "[UPSTOX_OAUTH_STATE_EXPIRED] OAuth state expired (${age / 1000}s old)")
            activeStates.remove(incomingState)
            return false
        }

        // Mark as consumed & remove from active tracking
        entry.consumed = true
        activeStates.remove(incomingState)
        Log.i(TAG, "[UPSTOX_OAUTH_STATE_VALIDATED] OAuth state successfully validated and consumed")
        return true
    }

    /**
     * Validates whether an authorization code has already been exchanged (code single-use guard).
     */
    fun validateAndConsumeAuthCode(authCode: String): Boolean {
        val cleanCode = authCode.trim()
        if (cleanCode.isBlank()) return false
        if (consumedAuthCodes.contains(cleanCode)) {
            Log.w(TAG, "[UPSTOX_CODE_REPLAY_PREVENTED] Authorization code has already been consumed!")
            return false
        }
        consumedAuthCodes.add(cleanCode)
        // Keep set bound
        if (consumedAuthCodes.size > 100) {
            consumedAuthCodes.clear()
        }
        return true
    }

    fun getAuthorizationUrl(
        apiKey: String,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        state: String? = null
    ): String {
        val targetRedirect = if (redirectUri.isBlank() || redirectUri.startsWith("kingkhan://")) {
            DEFAULT_REDIRECT_URI
        } else {
            redirectUri
        }
        val encodedRedirect = URLEncoder.encode(targetRedirect, "UTF-8")
        val effectiveState = state ?: generateSecureState()
        val encodedState = URLEncoder.encode(effectiveState, "UTF-8")
        return "$AUTH_DIALOG_BASE?response_type=code&client_id=$apiKey&redirect_uri=$encodedRedirect&state=$encodedState"
    }

    fun buildLoginUrl(
        apiKey: String,
        redirectUri: String = DEFAULT_REDIRECT_URI,
        state: String? = null
    ): String = getAuthorizationUrl(apiKey, redirectUri, state)
}

