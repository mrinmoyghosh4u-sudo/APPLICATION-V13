package com.example.data.network

import android.util.Log
import com.example.data.model.MarketDataSourceNames
import com.example.data.model.MarketDataStore
import com.example.data.model.WatchlistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Yahoo Finance Reference Data Provider
 * 
 * Note: Yahoo Finance provides REFERENCE / DELAYED data and is NEVER used as a live trading tick feed.
 */
object YahooFinanceService {
    private const val TAG = "YahooFinanceRef"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Map symbols to Yahoo Finance tickers
    private val symbolMap = mapOf(
        "NIFTY 50" to "^NSEI",
        "BANKNIFTY" to "^NSEBANK",
        "FINNIFTY" to "NIFTY_FIN_SERVICE.NS",
        "MIDCPNIFTY" to "^NSEMDCP50",
        "SENSEX" to "^BSESN",
        "BANKEX" to "BSE-BANK.BO",
        "CRUDEOIL" to "CL=F",
        "CRUDEOIL M" to "CL=F"
    )

    suspend fun getMarketQuotes(symbols: List<String>): List<WatchlistItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<WatchlistItem>()
        
        for (symbol in symbols) {
            val yahooSymbol = symbolMap[symbol.replace(" 50", " 50").trim()]
            if (yahooSymbol != null) {
                try {
                    val request = Request.Builder()
                        .url("https://query1.finance.yahoo.com/v8/finance/chart/$yahooSymbol?interval=1d&range=1d")
                        .header("User-Agent", "Mozilla/5.0")
                        .build()
                        
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (body != null) {
                                val json = JSONObject(body)
                                val chart = json.getJSONObject("chart")
                                val resultArr = chart.getJSONArray("result")
                                if (resultArr.length() > 0) {
                                    val data = resultArr.getJSONObject(0)
                                    val meta = data.getJSONObject("meta")
                                    val ltp = meta.getDouble("regularMarketPrice")
                                    val prevClose = meta.getDouble("chartPreviousClose")
                                    val change = ltp - prevClose
                                    val changePercent = if (prevClose > 0) (change / prevClose) * 100 else 0.0
                                    
                                    val exchange = when {
                                        symbol.contains("CRUDE") -> "MCX"
                                        symbol.contains("SENSEX") || symbol.contains("BANKEX") -> "BSE"
                                        else -> "NSE"
                                    }

                                    if (ltp > 0.0) {
                                        MarketDataStore.updateTick(
                                            source = MarketDataSourceNames.YAHOO,
                                            symbol = symbol,
                                            token = "",
                                            exchange = exchange,
                                            ltp = ltp,
                                            close = prevClose,
                                            receivedTimestamp = System.currentTimeMillis(),
                                            state = "REFERENCE"
                                        )
                                        
                                        result.add(
                                            WatchlistItem(
                                                symbol = symbol,
                                                exchange = exchange,
                                                ltp = ltp,
                                                change = change,
                                                changePercent = changePercent,
                                                lotSize = com.example.util.AppPreferences.getGlobalLotSize(symbol),
                                                isPositive = change >= 0
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error fetching Yahoo reference quote for $symbol: ${e.localizedMessage}")
                }
            }
        }
        return@withContext result
    }
}
