package com.example.data.model

data class CanonicalInstrument(
    val exchange: String,
    val segment: String,
    val symbol: String,
    val displayName: String,
    val instrumentType: String,
    val instrumentKey: String,
    val token: String,
    val expiry: String = "",
    val strike: Double = 0.0,
    val optionType: String = "",
    val lotSize: Int = 1
)
