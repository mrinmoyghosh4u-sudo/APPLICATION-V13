package com.example.data.model

sealed class AccountDataState<out T> {
    object Loading : AccountDataState<Nothing>()
    data class Success<out T>(val data: T) : AccountDataState<T>()
    object Empty : AccountDataState<Nothing>()
    data class Error(val message: String) : AccountDataState<Nothing>()
    data class Stale<out T>(val data: T, val message: String) : AccountDataState<T>()
}
