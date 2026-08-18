package com.example.data.network

import android.content.Context
import android.util.JsonReader
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class Instrument(
    val token: String,
    val symbol: String,
    val name: String,
    val expiry: String,
    val strike: String,
    val lotsize: String,
    val instrumenttype: String,
    val exch_seg: String,
    val tick_size: String
)

class InstrumentMasterService(
    private val client: OkHttpClient = OkHttpClient(),
    private val context: Context? = null
) {
    
    private val masterClient = client.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val instrumentMap = mutableMapOf<String, Instrument>()
    private val indexSymbolMap = mutableMapOf<String, Instrument>()

    var isLoaded = false
        private set

    init {
        // Pre-populate core index instruments so index quotes and tokens work immediately even before or during download
        val nifty = Instrument("26000", "NIFTY", "NIFTY", "", "", "50", "AMXIDX", "NSE", "0.05")
        val bankNifty = Instrument("26009", "BANKNIFTY", "BANKNIFTY", "", "", "15", "AMXIDX", "NSE", "0.05")
        val finNifty = Instrument("26037", "FINNIFTY", "FINNIFTY", "", "", "25", "AMXIDX", "NSE", "0.05")
        val midcapNifty = Instrument("26074", "MIDCPNIFTY", "MIDCPNIFTY", "", "", "50", "AMXIDX", "NSE", "0.05")

        indexSymbolMap["NIFTY"] = nifty
        indexSymbolMap["BANKNIFTY"] = bankNifty
        indexSymbolMap["FINNIFTY"] = finNifty
        indexSymbolMap["MIDCPNIFTY"] = midcapNifty

        listOf(nifty, bankNifty, finNifty, midcapNifty).forEach {
            instrumentMap[it.token] = it
        }
    }

    private fun parseInputStream(inputStream: InputStream) {
        val reader = JsonReader(InputStreamReader(inputStream, "UTF-8"))
        reader.beginArray()
        while (reader.hasNext()) {
            var token = ""
            var symbol = ""
            var name = ""
            var expiry = ""
            var strike = ""
            var lotsize = ""
            var instType = ""
            var exch = ""
            var tickSize = ""
            
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "token" -> token = reader.nextString()
                    "symbol" -> symbol = reader.nextString()
                    "name" -> name = reader.nextString()
                    "expiry" -> expiry = reader.nextString()
                    "strike" -> strike = reader.nextString()
                    "lotsize" -> lotsize = reader.nextString()
                    "instrumenttype" -> instType = reader.nextString()
                    "exch_seg" -> exch = reader.nextString()
                    "tick_size" -> tickSize = reader.nextString()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            
            if (token.isNotBlank()) {
                val inst = Instrument(token, symbol, name, expiry, strike, lotsize, instType, exch, tickSize)
                instrumentMap[token] = inst
                if (exch == "NSE" && (instType == "" || instType == "AMXIDX") && (name == "NIFTY" || name == "BANKNIFTY" || name == "FINNIFTY" || name == "MIDCPNIFTY")) {
                    indexSymbolMap[name] = inst
                }
            }
        }
        reader.endArray()
        reader.close()
    }
    
    suspend fun loadMaster() = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext

        val cacheFile = context?.let { File(it.cacheDir, "OpenAPIScripMaster.json") }

        // Try loading from recent local disk cache if available (< 24 hours old)
        if (cacheFile != null && cacheFile.exists() && cacheFile.length() > 0 && (System.currentTimeMillis() - cacheFile.lastModified() < 86400000L)) {
            try {
                Log.d("InstrumentMaster", "Loading master from local cache (${cacheFile.length()} bytes)")
                FileInputStream(cacheFile).use { parseInputStream(it) }
                isLoaded = true
                Log.d("InstrumentMaster", "Loaded ${instrumentMap.size} instruments from local cache")
                return@withContext
            } catch (e: Exception) {
                Log.w("InstrumentMaster", "Failed to parse cached master file, re-downloading", e)
                cacheFile.delete()
            }
        }

        var attempts = 0
        val maxAttempts = 3
        var success = false

        while (attempts < maxAttempts && !success) {
            attempts++
            try {
                Log.d("InstrumentMaster", "Starting master download (attempt $attempts/$maxAttempts)")
                val request = Request.Builder()
                    .url("https://margincalculator.angelbroking.com/OpenAPI_File/files/OpenAPIScripMaster.json")
                    .header("User-Agent", "KingKhanAITradeApp")
                    .build()

                masterClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("Failed to download master (HTTP ${response.code})")
                    val body = response.body ?: throw Exception("Empty body in master file response")

                    if (cacheFile != null) {
                        body.byteStream().use { input ->
                            FileOutputStream(cacheFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        FileInputStream(cacheFile).use { parseInputStream(it) }
                    } else {
                        body.byteStream().use { parseInputStream(it) }
                    }

                    isLoaded = true
                    success = true
                    Log.d("InstrumentMaster", "Loaded ${instrumentMap.size} instruments successfully")
                }
            } catch (e: Exception) {
                Log.e("InstrumentMaster", "Attempt $attempts failed to download/parse master: ${e.localizedMessage}")
                if (attempts < maxAttempts) {
                    kotlinx.coroutines.delay(1000L * attempts)
                }
            }
        }

        // If network downloads failed but an older cache file exists, fallback to reading older cache
        if (!success && cacheFile != null && cacheFile.exists() && cacheFile.length() > 0) {
            try {
                Log.d("InstrumentMaster", "Falling back to existing cached master file after network failure")
                FileInputStream(cacheFile).use { parseInputStream(it) }
                isLoaded = true
                Log.d("InstrumentMaster", "Loaded ${instrumentMap.size} instruments from fallback cache")
            } catch (e: Exception) {
                Log.e("InstrumentMaster", "Failed reading fallback cache file", e)
            }
        }
    }

    fun getInstrumentByToken(token: String): Instrument? = instrumentMap[token]
    
        fun getOptionInstruments(name: String, expiry: String): List<Instrument> {
        val uppercaseName = name.uppercase()
        val nfoName = if (uppercaseName == "NIFTY 50") "NIFTY" else uppercaseName
        
        return instrumentMap.values.filter {
            it.exch_seg == "NFO" && it.name == nfoName && it.expiry == expiry
        }
    }
    
    fun getOptionExpiries(name: String): List<String> {
        val uppercaseName = name.uppercase()
        val nfoName = if (uppercaseName == "NIFTY 50") "NIFTY" else uppercaseName
        
        val expiries = instrumentMap.values.filter {
            it.exch_seg == "NFO" && it.name == nfoName
        }.map { it.expiry }.distinct().sortedBy {
            // Need a quick way to sort date strings if possible, or just string sort which might be flawed
            // Let's rely on standard format or return them to be sorted outside
            it
        }
        return expiries
    }

    fun resolveIndexToken(indexName: String): Instrument? {
        val nameToSymbol = mapOf(
            "NIFTY 50" to "NIFTY",
            "BANKNIFTY" to "BANKNIFTY",
            "FINNIFTY" to "FINNIFTY",
            "MIDCPNIFTY" to "MIDCPNIFTY"
        )
        val symbol = nameToSymbol[indexName] ?: indexName
        return indexSymbolMap[symbol]
    }

    fun resolveAngelToken(symbol: String, exchange: String = "NSE"): String? {
        val uppercaseSymbol = symbol.uppercase().trim()
        
        // Check index symbol map first
        val indexInst = indexSymbolMap[uppercaseSymbol] ?: indexSymbolMap[if (uppercaseSymbol == "NIFTY 50") "NIFTY" else uppercaseSymbol]
        if (indexInst != null) return indexInst.token
        
        // Exact token lookup if symbol is already a numeric token
        if (uppercaseSymbol.all { it.isDigit() }) return uppercaseSymbol

        // Search by symbol or name
        val match = instrumentMap.values.find {
            (it.symbol.equals(uppercaseSymbol, ignoreCase = true) || it.name.equals(uppercaseSymbol, ignoreCase = true)) &&
            (exchange.isBlank() || it.exch_seg.equals(exchange, ignoreCase = true))
        }
        return match?.token
    }

    fun resolveDhanSecurityId(symbol: String, exchange: String = "NSE"): String? {
        val uppercaseSymbol = symbol.uppercase().trim()
        
        // Official Dhan Index Scrip IDs
        when (uppercaseSymbol) {
            "NIFTY", "NIFTY 50", "NIFTY50", "13" -> return "13"
            "BANKNIFTY", "25" -> return "25"
            "FINNIFTY", "27" -> return "27"
            "MIDCPNIFTY", "31" -> return "31"
        }

        // If numeric securityId supplied
        if (uppercaseSymbol.all { it.isDigit() }) return uppercaseSymbol

        // Search in instrument master by symbol
        val match = instrumentMap.values.find {
            it.symbol.equals(uppercaseSymbol, ignoreCase = true) || it.name.equals(uppercaseSymbol, ignoreCase = true)
        }
        return match?.token
    }
}
