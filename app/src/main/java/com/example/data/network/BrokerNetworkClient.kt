package com.example.data.network

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class BrokerNetworkClient(private val sessionManager: SessionManager) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // 10. If any API call fails, log: Endpoint and HTTP Status Code safely without sensitive data
    private val errorLoggingInterceptor = Interceptor { chain ->
        val request = chain.request()
        try {
            val response = chain.proceed(request)
            if (!response.isSuccessful) {
                if (response.code == 401 || response.code == 403) {
                    Log.w("BrokerAPI", "API Auth Required (${response.code}): ${request.url.encodedPath}")
                } else {
                    Log.e("BrokerAPI", "API FAILED: ${request.url.encodedPath} | Status: ${response.code}")
                }
            }
            response
        } catch (e: Exception) {
            Log.e("BrokerAPI", "API EXCEPTION: ${request.url.encodedPath} | Exception: ${e.message}")
            throw e
        }
    }

    // 11. Safe retry interceptor (ONLY for GET requests)
    private val retryInterceptor = Interceptor { chain ->
        val request = chain.request()
        // NEVER automatically retry order placement, modification, or cancellation requests (POST, PUT, DELETE)
        if (request.method != "GET") {
            return@Interceptor chain.proceed(request)
        }

        var response: Response? = null
        var tryCount = 0
        val maxRetries = 3
        var exception: Exception? = null

        while (tryCount < maxRetries && (response == null || !response.isSuccessful)) {
            try {
                if (response != null) {
                    response.close()
                }
                response = chain.proceed(request)
            } catch (e: Exception) {
                exception = e
            } finally {
                tryCount++
            }
            
            // Retry on 5xx or specific network errors
            if (response?.isSuccessful == false && response.code < 500) {
                break // don't retry 4xx errors
            }
            if (tryCount < maxRetries && (response == null || !response.isSuccessful)) {
                try {
                    Thread.sleep(1000L * tryCount)
                } catch (ie: InterruptedException) {
                    // ignore
                }
            }
        }
        
        if (response == null) {
            throw exception ?: IOException("Failed to execute request after $maxRetries retries")
        }
        response
    }

    // 12. Validate the access token before every request and refresh it if required.
    private val dhanAuthInterceptor = Interceptor { chain ->
        val token = sessionManager.dhanAccessToken?.trim() ?: ""
        val clientId = sessionManager.dhanClientId?.trim() ?: ""
        
        if (token.isBlank()) {
            throw IOException("Dhan Access Token is missing or expired")
        }

        val requestBuilder = chain.request().newBuilder()
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("access-token", token)

        if (clientId.isNotBlank()) {
            requestBuilder.header("client-id", clientId)
        }

        val request = requestBuilder.build()
        val response = chain.proceed(request)

        if (response.code == 401 || response.code == 403) {
            Log.w("DhanAPI", "Dhan access token expired or invalid (${response.code}). Please reconnect your Dhan account.")
            sessionManager.isDhanConnected = false
        }
        response
    }

    // Angel One OkHttpClient
    private val angelOkHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
        val authInterceptor = Interceptor { chain ->
            val apiKey = sessionManager.angelApiKey.takeIf { it.isNotBlank() }
                ?: runCatching { com.example.BuildConfig.ANGEL_ONE_API_KEY }.getOrNull()
                    ?.takeIf { it.isNotBlank() && it != "ANGEL_ONE_API_KEY_DEFAULT_VALUE" }
                ?: "ANGEL_ONE_SMART_API_KEY"

            val requestBuilder = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("X-UserType", "USER")
                .addHeader("X-SourceID", "WEB")
                .addHeader("X-ClientLocalIP", "127.0.0.1")
                .addHeader("X-ClientPublicIP", "127.0.0.1")
                .addHeader("X-MACAddress", "00:00:00:00:00:00")
                .addHeader("X-PrivateKey", apiKey)

            val token = sessionManager.angelJwtToken?.takeIf { it.isNotBlank() }
                ?: sessionManager.angelAuthToken?.takeIf { it.isNotBlank() }
            if (!token.isNullOrBlank()) {
                val bearerToken = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
                requestBuilder.addHeader("Authorization", bearerToken)
            }
            chain.proceed(requestBuilder.build())
        }

        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    // Dhan OkHttpClient
    private val dhanOkHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(dhanAuthInterceptor)
            .addInterceptor(retryInterceptor)
            .addInterceptor(errorLoggingInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val angelOneApi: AngelOneApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://apiconnect.angelone.in/")
            .client(angelOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AngelOneApi::class.java)
    }

    val dhanApi: DhanApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.dhan.co/")
            .client(dhanOkHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DhanApi::class.java)
    }

    // =========================================
    // FYERS
    // =========================================
    private val fyersClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(errorLoggingInterceptor)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
            
            // Fyers usually doesn't need Bearer here unless we call other APIs.
            // But if we do:
            // sessionManager.fyersAccessToken?.let {
            //     requestBuilder.header("Authorization", "$appId:$it")
            // }
            
            chain.proceed(requestBuilder.build())
        }
        .build()

    val fyersApi: FyersApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api-t1.fyers.in/")
            .client(fyersClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FyersApi::class.java)
    }

    // =========================================
    // UPSTOX (PRIMARY MARKET DATA)
    // =========================================
    private val upstoxClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(errorLoggingInterceptor)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Accept", "application/json")
            chain.proceed(requestBuilder.build())
        }
        .build()

    val upstoxApi: UpstoxApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api-v2.upstox.com/")
            .client(upstoxClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(UpstoxApi::class.java)
    }
}
