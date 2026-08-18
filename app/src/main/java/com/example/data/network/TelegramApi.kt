package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class TelegramSendMessageRequest(
    @Json(name = "chat_id") val chatId: String,
    @Json(name = "text") val text: String,
    @Json(name = "parse_mode") val parseMode: String = "HTML"
)

@JsonClass(generateAdapter = true)
data class TelegramSendMessageResponse(
    @Json(name = "ok") val ok: Boolean,
    @Json(name = "description") val description: String? = null,
    @Json(name = "error_code") val errorCode: Int? = null,
    @Json(name = "result") val result: Any? = null
)

interface TelegramApi {
    @POST("bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path("token") token: String,
        @Body request: TelegramSendMessageRequest
    ): Response<TelegramSendMessageResponse>
}
