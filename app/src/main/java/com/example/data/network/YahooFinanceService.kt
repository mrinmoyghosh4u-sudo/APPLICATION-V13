package com.example.data.network

import com.example.data.model.WatchlistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object YahooFinanceService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Map our symbols to Yahoo Finance symbols
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
            val yahooSymbol = symbolMap[symbol.replace(" 50", " 50").trim()] // simple match
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
                                    val changePercent = (change / prevClose) * 100
                                    
                                    val exchange = when {
                                        symbol.contains("CRUDE") -> "MCX"
                                        symbol.contains("SENSEX") || symbol.contains("BANKEX") -> "BSE"
                                        else -> "NSE"
                                    }
                                    
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
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return@withContext result
    }
}
