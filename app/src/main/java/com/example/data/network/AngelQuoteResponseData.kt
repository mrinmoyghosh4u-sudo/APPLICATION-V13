package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AngelQuoteResponseData(
    @Json(name = "fetched") val fetched: List<AngelQuoteItem>? = emptyList(),
    @Json(name = "unfetched") val unfetched: List<AngelUnfetchedQuote>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class AngelUnfetchedQuote(
    @Json(name = "exchange") val exchange: String? = null,
    @Json(name = "symbolToken") val symbolToken: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "errorCode") val errorCode: String? = null
)

