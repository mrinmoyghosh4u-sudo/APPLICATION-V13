package com.example.data.network

fun Any?.toStringOrDefault(default: String = ""): String {
    if (this == null) return default
    val str = this.toString()
    return if (str.isBlank() || str == "null") default else str
}

fun Any?.toDoubleOrDefault(default: Double = 0.0): Double {
    if (this == null) return default
    if (this is Double) return this
    if (this is Number) return this.toDouble()
    return this.toString().toDoubleOrNull() ?: default
}

fun Any?.toIntOrDefault(default: Int = 0): Int {
    if (this == null) return default
    if (this is Int) return this
    if (this is Number) return this.toInt()
    return this.toString().toDoubleOrNull()?.toInt() ?: default
}
