cat << 'INNER_EOF' > app/src/main/java/com/example/viewmodel/MainViewModel.kt
package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.TradingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.example.data.network.AngelOneBrokerService
import com.example.data.network.AngelOneApi
import com.example.data.network.DhanBrokerService
import com.example.data.network.DhanApi
import com.example.data.network.BrokerNetworkClient
import com.example.data.network.SessionManager
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class MainViewModel(
    private val repository: TradingRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val brokerNetworkClient = BrokerNetworkClient(sessionManager)
    
    private val angelApi = brokerNetworkClient.buildRetrofit("https://apiconnect.angelbroking.com/", moshi)
        .create(AngelOneApi::class.java)
        
    private val dhanApi = brokerNetworkClient.buildRetrofit("https://api.dhan.co/v2/", moshi)
        .create(DhanApi::class.java)

    // Using Dhan integration as requested
    private val brokerService = DhanBrokerService("Dhan", dhanApi, sessionManager)
    // private val brokerService = AngelOneBrokerService(angelApi, sessionManager) // Old AngelOne service

    private val _uiState = MutableStateFlow(TradingUiState())
    val uiState: StateFlow<TradingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.uiState.collect { state ->
                _uiState.value = state
            }
        }
        
        // Initial load
        viewModelScope.launch {
            if (sessionManager.dhanAccessToken != null || sessionManager.angelAccessToken != null) {
                syncWithBroker()
                startMarketDataPolling()
            }
        }
    }

    private fun startMarketDataPolling() {
        viewModelScope.launch {
            while(true) {
                syncWithBroker()
                getOptionChainStrikes()
                delay(5000L) // Poll every 5 seconds as per requirements
            }
        }
    }

    private suspend fun syncWithBroker() {
        try {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.syncWithBroker(brokerService)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message ?: "Failed to sync with broker") }
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun login(clientCode: String, pin: String, totp: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                // Implementation would depend on whether it's Angel or Dhan
                // For Dhan, we usually use OAuth flow, this is just a placeholder for the legacy Angel UI
                _uiState.update { it.copy(error = "Dhan uses OAuth flow. Please use the Web login.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Login failed") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _uiState.update { it.copy(
                userProfile = UserProfileEntity(
                    id = 1,
                    name = "Guest",
                    email = "",
                    availableMargin = 0.0,
                    accountBalance = 0.0,
                    todaysPnl = 0.0,
                    todaysPnlPercent = 0.0,
                    connectedBroker = ""
                ),
                holdings = emptyList(),
                orders = emptyList(),
                optionChain = emptyList()
            )}
        }
    }

    fun placeOrder(
        symbol: String,
        exchange: String,
        action: String,
        type: String,
        qty: Int,
        price: Double,
        productType: String,
        stopLoss: Double = 0.0,
        target: Double = 0.0
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val orderId = brokerService.placeOrder(
                    symbol, exchange, action, type, qty, price, productType, stopLoss, target
                ).getOrThrow()
                
                // Immediately sync after placing order to reflect changes
                syncWithBroker()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to place order") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getOptionChainStrikes() {
        viewModelScope.launch {
            try {
                // Hardcoded NIFTY expiry for demo, should be dynamic in prod
                val strikes = brokerService.getOptionChain("NIFTY", "2024-05-30").getOrThrow()
                repository.updateOptionChain(strikes)
            } catch (e: Exception) {
                // Log quietly
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val db = com.example.data.local.AppDatabase.getDatabase(context)
                val session = SessionManager(context)
                val repository = TradingRepository(db.tradingDao())
                return MainViewModel(repository, session) as T
            }
        }
    }
}
INNER_EOF
