package com.example.util

import android.util.Log
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Standard RFC 6238 Time-Based One-Time Password (TOTP) generator.
 * Generates official 6-digit TOTP tokens from Base32-encoded secrets.
 */
object TotpUtil {
    private const val TAG = "TotpUtil"
    private const val TIME_STEP_SECONDS = 30L

    /**
     * Generates a 6-digit TOTP code for the given Base32 secret at the current time (or specified timestamp).
     */
    fun generateTotp(secretBase32: String, timeMillis: Long = System.currentTimeMillis()): String {
        val cleanSecret = secretBase32.replace(" ", "").replace("-", "").trim().uppercase()
        if (cleanSecret.isBlank()) return ""

        return try {
            val keyBytes = decodeBase32(cleanSecret)
            if (keyBytes.isEmpty()) {
                Log.e(TAG, "Failed to decode Base32 secret")
                return ""
            }

            val timeStep = (timeMillis / 1000L) / TIME_STEP_SECONDS
            val data = ByteBuffer.allocate(8).putLong(timeStep).array()

            val mac = Mac.getInstance("HmacSHA1")
            mac.init(SecretKeySpec(keyBytes, "HmacSHA1"))
            val hash = mac.doFinal(data)

            val offset = hash[hash.size - 1].toInt() and 0x0F
            val truncatedHash = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)

            val pinValue = truncatedHash % 1_000_000
            String.format(java.util.Locale.US, "%06d", pinValue)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating TOTP: ${e.message}", e)
            ""
        }
    }

    /**
     * Pure Kotlin Base32 decoder supporting standard RFC 4648 Base32 alphabet.
     */
    private fun decodeBase32(base32: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val trimmed = base32.trimEnd('=')
        var buffer = 0
        var bitsLeft = 0
        val out = mutableListOf<Byte>()

        for (c in trimmed) {
            val charIndex = alphabet.indexOf(c)
            if (charIndex < 0) continue // Skip invalid characters (e.g. whitespace, padding)
            buffer = (buffer shl 5) or (charIndex and 31)
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                out.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }
}
