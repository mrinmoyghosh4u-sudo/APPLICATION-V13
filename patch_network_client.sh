cat << 'INNER_EOF' > app/src/main/java/com/example/data/network/BrokerNetworkClient.kt
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

    // 10. If any API call fails, log: Endpoint, HTTP Status Code, Response Body, Exception
    private val errorLoggingInterceptor = Interceptor { chain ->
        val request = chain.request()
        try {
            val response = chain.proceed(request)
            if (!response.isSuccessful) {
                val responseBody = response.peekBody(Long.MAX_VALUE).string()
                Log.e("DhanAPI", "API FAILED: ${request.url} | Status: ${response.code} | Body: $responseBody")
            }
            response
        } catch (e: Exception) {
            Log.e("DhanAPI", "API EXCEPTION: ${request.url} | Exception: ${e.message}", e)
            throw e
        }
    }

    // 11. Retry failed API requests automatically.
    private val retryInterceptor = Interceptor { chain ->
        var request = chain.request()
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
        var token = sessionManager.dhanAccessToken
        val clientId = sessionManager.dhanClientId
        
        if (token.isNullOrBlank()) {
            throw IOException("Dhan Access Token is missing or expired")
        }

        val requestBuilder = chain.request().newBuilder()
            .addHeader("Content-Type", "application/json")
            .addHeader("client-id", clientId)
            .addHeader("access-token", token)

        val request = requestBuilder.build()
        var response = chain.proceed(request)

        if (response.code == 401 || response.code == 403) {
            // Attempt to fetch from environment if available or clear session
            val envToken = runCatching { com.example.BuildConfig.DHAN_API_KEY as String? }.getOrNull()
            if (!envToken.isNullOrBlank() && envToken != "DHAN_API_KEY_DEFAULT_VALUE" && envToken != token) {
                sessionManager.dhanAccessToken = envToken
                token = envToken
                response.close()
                val newRequest = request.newBuilder()
                    .header("access-token", token)
                    .build()
                response = chain.proceed(newRequest)
            } else {
                Log.e("DhanAPI", "Token expired or invalid, please re-authenticate")
                // Cannot refresh Dhan token without user intervention in this setup
            }
        }
        response
    }

    // Angel One OkHttpClient
    private val angelOkHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val authInterceptor = Interceptor { chain ->
            val apiKey = sessionManager.angelApiKey.ifBlank {
                runCatching { com.example.BuildConfig.ANGEL_ONE_API_KEY }.getOrNull()
                    ?.takeIf { it.isNotBlank() && it != "ANGEL_ONE_API_KEY_DEFAULT_VALUE" }
                    ?: "ANGEL_ONE_SMART_API_KEY"
            }
            val requestBuilder = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("X-UserType", "USER")
                .addHeader("X-SourceID", "WEB")
                .addHeader("X-ClientLocalIP", "127.0.0.1")
                .addHeader("X-ClientPublicIP", "127.0.0.1")
                .addHeader("X-MACAddress", "00:00:00:00:00:00")
                .addHeader("X-PrivateKey", apiKey)

            sessionManager.angelJwtToken?.let { token ->
                if (token.isNotBlank()) {
                    val bearerToken = if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token"
                    requestBuilder.addHeader("Authorization", bearerToken)
                }
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
            level = HttpLoggingInterceptor.Level.BODY
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
}
INNER_EOF
