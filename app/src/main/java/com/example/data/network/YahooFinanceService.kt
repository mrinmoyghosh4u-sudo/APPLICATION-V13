package com.example.data.network

import android.util.Log
import com.example.data.model.HistoricalCandle
import com.example.data.model.MarketBreadth
import com.example.data.model.MarketDataSourceNames
import com.example.data.model.MarketDataStore
import com.example.data.model.OptionStrikeItem
import com.example.data.model.WatchlistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Yahoo Finance Reference Data Provider
 * 
 * Provides delayed reference quotes, historical candle data fallback,
 * and market breadth calculations.
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
        "NIFTY" to "^NSEI",
        "BANKNIFTY" to "^NSEBANK",
        "FINNIFTY" to "NIFTY_FIN_SERVICE.NS",
        "MIDCPNIFTY" to "^NSEMDCP50",
        "SENSEX" to "^BSESN",
        "BANKEX" to "BSE-BANK.BO",
        "CRUDEOIL" to "CL=F",
        "CRUDEOIL M" to "CL=F",
        "GOLD" to "GC=F",
        "GOLD M" to "GC=F",
        "SILVER" to "SI=F",
        "SILVER M" to "SI=F",
        "COPPER" to "HG=F",
        "COPPER M" to "HG=F",
        "RELIANCE" to "RELIANCE.NS",
        "TCS" to "TCS.NS",
        "INFY" to "INFY.NS",
        "SBIN" to "SBIN.NS",
        "HDFCBANK" to "HDFCBANK.NS",
        "ICICIBANK" to "ICICIBANK.NS",
        "TATAMOTORS" to "TATAMOTORS.NS",
        "TATASTEEL" to "TATASTEEL.NS"
    )

    fun getYahooSymbol(symbol: String): String? {
        val norm = symbol.trim().uppercase()
        return symbolMap[norm] ?: symbolMap[norm.replace(" ", "")] ?: if (!norm.contains("^") && !norm.endsWith(".NS") && !norm.endsWith(".BO")) "$norm.NS" else norm
    }

    suspend fun getMarketQuotes(symbols: List<String>): List<WatchlistItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<WatchlistItem>()
        val usdInrRate = 87.2 // Benchmark USD/INR conversion rate for MCX commodity contracts
        
        for (symbol in symbols) {
            val yahooSymbol = getYahooSymbol(symbol)
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
                                    var rawLtp = meta.optDouble("regularMarketPrice", 0.0)
                                    var rawPrevClose = meta.optDouble("chartPreviousClose", 0.0)
                                    
                                    val upperSym = symbol.uppercase()
                                    // Scale international futures to Indian MCX standard INR contract sizes
                                    val (ltp, prevClose) = when {
                                        upperSym.startsWith("CRUDEOIL") -> {
                                            // Crude Oil 1 bbl in INR
                                            (rawLtp * usdInrRate) to (rawPrevClose * usdInrRate)
                                        }
                                        upperSym.startsWith("GOLD") -> {
                                            // Gold COMEX USD/oz to MCX Gold 10g INR (1 oz = 31.1035g + duty multiplier)
                                            val factor = (10.0 / 31.1035) * usdInrRate * 1.12
                                            (rawLtp * factor) to (rawPrevClose * factor)
                                        }
                                        upperSym.startsWith("SILVER") -> {
                                            // Silver COMEX USD/oz to MCX Silver 1kg INR
                                            val factor = (1000.0 / 31.1035) * usdInrRate * 1.08
                                            (rawLtp * factor) to (rawPrevClose * factor)
                                        }
                                        upperSym.startsWith("COPPER") -> {
                                            // Copper COMEX USD/lb to MCX Copper 1kg INR (1 kg = 2.20462 lbs)
                                            val factor = 2.20462 * usdInrRate
                                            (rawLtp * factor) to (rawPrevClose * factor)
                                        }
                                        else -> rawLtp to rawPrevClose
                                    }

                                    val change = if (prevClose > 0.0) ltp - prevClose else 0.0
                                    val changePercent = if (prevClose > 0.0) (change / prevClose) * 100.0 else 0.0
                                    
                                    val exchange = when {
                                        upperSym.contains("CRUDE") || upperSym.contains("GOLD") || upperSym.contains("SILVER") || upperSym.contains("COPPER") -> "MCX"
                                        upperSym.contains("SENSEX") || upperSym.contains("BANKEX") -> "BSE"
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

    suspend fun getHistoricalCandles(symbol: String, interval: String = "15m"): Result<List<HistoricalCandle>> = withContext(Dispatchers.IO) {
        runCatching {
            val yahooSymbol = getYahooSymbol(symbol) ?: throw Exception("Symbol $symbol not mapped for Yahoo")
            val (yahooInterval, range) = when (interval.lowercase()) {
                "1m" -> Pair("1m", "1d")
                "5m" -> Pair("5m", "5d")
                "15m" -> Pair("15m", "5d")
                "30m" -> Pair("30m", "1mo")
                "1h" -> Pair("60m", "1mo")
                "1d" -> Pair("1d", "3mo")
                else -> Pair("15m", "5d")
            }

            val url = "https://query1.finance.yahoo.com/v8/finance/chart/$yahooSymbol?interval=$yahooInterval&range=$range"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Yahoo historical candles HTTP error: ${response.code}")
                }
                val body = response.body?.string() ?: throw Exception("Empty body from Yahoo")
                val json = JSONObject(body)
                val chart = json.getJSONObject("chart")
                val resultArr = chart.getJSONArray("result")
                if (resultArr.length() == 0) {
                    throw Exception("No historical candle data returned for $symbol")
                }
                val resultObj = resultArr.getJSONObject(0)
                val timestamps = resultObj.optJSONArray("timestamp") ?: throw Exception("Missing timestamp array")
                val indicators = resultObj.getJSONObject("indicators")
                val quoteArr = indicators.getJSONArray("quote")
                if (quoteArr.length() == 0) throw Exception("Missing quote indicators")
                val quote = quoteArr.getJSONObject(0)

                val opens = quote.optJSONArray("open")
                val highs = quote.optJSONArray("high")
                val lows = quote.optJSONArray("low")
                val closes = quote.optJSONArray("close")
                val volumes = quote.optJSONArray("volume")

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val candleList = mutableListOf<HistoricalCandle>()

                for (i in 0 until timestamps.length()) {
                    val ts = timestamps.getLong(i) * 1000L
                    val o = opens?.optDouble(i, Double.NaN) ?: Double.NaN
                    val h = highs?.optDouble(i, Double.NaN) ?: Double.NaN
                    val l = lows?.optDouble(i, Double.NaN) ?: Double.NaN
                    val c = closes?.optDouble(i, Double.NaN) ?: Double.NaN
                    val v = volumes?.optLong(i, 0L) ?: 0L

                    if (!o.isNaN() && !h.isNaN() && !l.isNaN() && !c.isNaN() && o > 0.0 && c > 0.0) {
                        candleList.add(
                            HistoricalCandle(
                                time = timeFormat.format(Date(ts)),
                                open = o,
                                high = h,
                                low = l,
                                close = c,
                                volume = v,
                                timestamp = ts
                            )
                        )
                    }
                }

                if (candleList.isEmpty()) {
                    throw Exception("All candles contained invalid or NaN data")
                }
                DataValidator.validateHistoricalCandles(candleList)
            }
        }
    }

    suspend fun getMarketBreadth(): Result<MarketBreadth> = withContext(Dispatchers.IO) {
        runCatching {
            val nifty50Constituents = listOf(
                "RELIANCE", "TCS", "HDFCBANK", "INFY", "ICICIBANK", "SBIN", "BHARTIARTL",
                "ITC", "KOTAKBANK", "LT", "HINDUNILVR", "AXISBANK", "BAJFINANCE", "ASIANPAINT",
                "MARUTI", "TITAN", "SUNPHARMA", "TATAMOTORS", "ULTRACEMCO", "TATASTEEL",
                "NTPC", "POWERGRID", "M&M", "ADANIENT", "JSWSTEEL", "COALINDIA", "BAJAJFINSV"
            )
            val quotes = getMarketQuotes(nifty50Constituents)
            if (quotes.isEmpty()) {
                throw Exception("Failed to fetch constituent data for breadth calculation")
            }

            var advances = 0
            var declines = 0
            var unchanged = 0

            quotes.forEach { item ->
                when {
                    item.change > 0.0 -> advances++
                    item.change < 0.0 -> declines++
                    else -> unchanged++
                }
            }

            MarketBreadth(
                advances = advances,
                declines = declines,
                unchanged = unchanged,
                total = quotes.size,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val yahooSymbol = getYahooSymbol(symbol) ?: throw Exception("Symbol $symbol not mapped for Yahoo Options")
            val url = "https://query1.finance.yahoo.com/v7/finance/options/$yahooSymbol"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Yahoo option chain HTTP error: ${response.code}")
                }
                val body = response.body?.string() ?: throw Exception("Empty body from Yahoo Options")
                val json = JSONObject(body)
                val optionChain = json.getJSONObject("optionChain")
                val resultArr = optionChain.getJSONArray("result")
                if (resultArr.length() == 0) {
                    throw Exception("No option chain result returned by Yahoo for $symbol")
                }
                val resultObj = resultArr.getJSONObject(0)
                val optionsArr = resultObj.optJSONArray("options") ?: throw Exception("No options array in Yahoo response")
                if (optionsArr.length() == 0) {
                    throw Exception("Empty options list in Yahoo response")
                }
                val optionData = optionsArr.getJSONObject(0)
                val calls = optionData.optJSONArray("calls")
                val puts = optionData.optJSONArray("puts")

                val strikesMap = mutableMapOf<Double, OptionStrikeItem>()

                if (calls != null) {
                    for (i in 0 until calls.length()) {
                        val c = calls.getJSONObject(i)
                        val strike = c.optDouble("strike", 0.0)
                        val ltp = c.optDouble("lastPrice", 0.0)
                        val oi = c.optLong("openInterest", 0L).toString()
                        val vol = c.optLong("volume", 0L).toString()
                        if (strike > 0.0) {
                            val current = strikesMap[strike] ?: OptionStrikeItem(strikePrice = strike)
                            strikesMap[strike] = current.copy(
                                callLtp = ltp,
                                callOi = oi,
                                callVolume = vol
                            )
                        }
                    }
                }

                if (puts != null) {
                    for (i in 0 until puts.length()) {
                        val p = puts.getJSONObject(i)
                        val strike = p.optDouble("strike", 0.0)
                        val ltp = p.optDouble("lastPrice", 0.0)
                        val oi = p.optLong("openInterest", 0L).toString()
                        val vol = p.optLong("volume", 0L).toString()
                        if (strike > 0.0) {
                            val current = strikesMap[strike] ?: OptionStrikeItem(strikePrice = strike)
                            strikesMap[strike] = current.copy(
                                putLtp = ltp,
                                putOi = oi,
                                putVolume = vol
                            )
                        }
                    }
                }

                val strikeList = strikesMap.values.sortedBy { it.strikePrice }
                if (strikeList.isEmpty()) {
                    throw Exception("Yahoo option chain returned 0 valid strike records")
                }
                strikeList
            }
        }
    }
}
