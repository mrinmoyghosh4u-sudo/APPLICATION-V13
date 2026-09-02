package com.example.data.model

import com.example.data.model.OptionChain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object OptionChainCache {
    private val CACHE_DURATION_MS = 60_000L // 1 minute max age if no live ticks

    private var cachedChain: OptionChain? = null
    private var lastFetchTime: Long = 0L
    private var lastProvider: String = ""

    private val _state = MutableStateFlow("UNAVAILABLE") // LOADING, AVAILABLE, STALE, UNAVAILABLE, ERROR
    val state: StateFlow<String> = _state.asStateFlow()

    fun get(symbol: String, expiry: String, provider: String): OptionChain? {
        val cache = cachedChain
        if (cache != null && cache.symbol == symbol && cache.expiry == expiry) {
            val now = System.currentTimeMillis()
            if (provider != lastProvider) {
                // Provider changed, invalidate
                invalidate()
                return null
            }
            if (now - lastFetchTime > CACHE_DURATION_MS) {
                _state.value = "STALE"
            }
            return cache
        }
        return null
    }

    fun update(chain: OptionChain, provider: String) {
        cachedChain = chain
        lastFetchTime = System.currentTimeMillis()
        lastProvider = provider
        _state.value = "AVAILABLE"
    }

    fun invalidate() {
        cachedChain = null
        lastFetchTime = 0L
        _state.value = "UNAVAILABLE"
    }

    fun setLoading() {
        _state.value = "LOADING"
    }

    fun setError() {
        _state.value = "ERROR"
    }
}
