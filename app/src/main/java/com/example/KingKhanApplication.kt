package com.example

import android.app.Application
import android.util.Log

class KingKhanApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupGlobalExceptionHandler()
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("KingKhanApp", "Uncaught exception on thread ${thread.name}", throwable)
            try {
                // If it's a known non-fatal background network / json parsing / coroutine exception, log and prevent hard crash
                val isNetworkOrParsing = throwable is java.io.IOException ||
                        throwable is org.json.JSONException ||
                        throwable is java.net.SocketTimeoutException ||
                        throwable is java.net.UnknownHostException ||
                        throwable is NullPointerException && thread.name.contains("DefaultDispatcher", ignoreCase = true)

                if (isNetworkOrParsing) {
                    Log.w("KingKhanApp", "Safely recovered from non-fatal background exception on thread: ${thread.name}")
                    return@setDefaultUncaughtExceptionHandler
                }
            } catch (e: Exception) {
                Log.e("KingKhanApp", "Error in uncaught exception handler", e)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
