package com.example.util

import java.security.MessageDigest

object FyersAuthHelper {
    fun generateAppIdHash(appId: String, secretId: String): String {
        val input = "$appId:$secretId"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
