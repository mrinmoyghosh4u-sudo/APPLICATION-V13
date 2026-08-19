package com.example.data.network

import android.content.Context
import android.util.JsonReader
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
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
    companion object {
        fun normalizeExchange(exchSeg: String): String {
            return when (exchSeg.trim().lowercase()) {
                "nse", "nse_cm", "nse-cm", "nse_eq" -> "NSE"
                "nfo", "nse_fo", "nse-fo", "nse_fno" -> "NFO"
                "bse", "bse_cm", "bse-cm", "bse_eq" -> "BSE"
                "bfo", "bse_fo", "bse-fo", "bse_fno" -> "BFO"
                "mcx", "mcx_fo", "mcx-fo", "mcx_cm", "mcx_comm" -> "MCX"
                "cds", "cde_fo" -> "CDS"
                "ncdex", "ncx_fo" -> "NCDEX"
                else -> exchSeg.trim().uppercase()
            }
        }

        fun getExchangeType(exchSeg: String): Int {
            return when (normalizeExchange(exchSeg)) {
                "NSE" -> 1
                "NFO" -> 2
                "BSE" -> 3
                "BFO" -> 4
                "MCX" -> 5
                "NCDEX" -> 7
                "CDS" -> 13
                else -> 1
            }
        }
    }
    
    private val masterClient = client.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Composite key: "$exchangeType:$token"
    private val instrumentMap = ConcurrentHashMap<String, Instrument>()
    private val symbolExchangeMap = ConcurrentHashMap<String, Instrument>()
    private val indexSymbolMap = ConcurrentHashMap<String, Instrument>()

    private val hardcodedIndices = mapOf(
        "NIFTY" to Instrument("99926000", "NIFTY 50", "NIFTY", "", "0", "50", "AMXIDX", "NSE", "0.05"),
        "BANKNIFTY" to Instrument("99926009", "NIFTY BANK", "BANKNIFTY", "", "0", "15", "AMXIDX", "NSE", "0.05"),
        "FINNIFTY" to Instrument("99926037", "NIFTY FIN SERVICE", "FINNIFTY", "", "0", "25", "AMXIDX", "NSE", "0.05"),
        "MIDCPNIFTY" to Instrument("99926074", "NIFTY MID SELECT", "MIDCPNIFTY", "", "0", "50", "AMXIDX", "NSE", "0.05"),
        "SENSEX" to Instrument("99919000", "SENSEX", "SENSEX", "", "0", "10", "AMXIDX", "BSE", "0.05"),
        "BANKEX" to Instrument("99919012", "BANKEX", "BANKEX", "", "0", "15", "AMXIDX", "BSE", "0.05"),
        "CRUDEOIL" to Instrument("240683", "CRUDEOIL", "CRUDEOIL", "", "0", "100", "FUTCOM", "MCX", "1.0"),
        "CRUDEOIL M" to Instrument("240684", "CRUDEOILM", "CRUDEOIL M", "", "0", "10", "FUTCOM", "MCX", "1.0")
    )

    private val _isLoaded = MutableStateFlow(false)
    val isLoadedFlow = _isLoaded.asStateFlow()
    val isLoaded: Boolean get() = _isLoaded.value

    init {
        // Pre-populate core index & standard instruments so lookups work immediately
        hardcodedIndices.forEach { (name, inst) ->
            val exchType = getExchangeType(inst.exch_seg)
            val compKey = "$exchType:${inst.token}"
            instrumentMap[compKey] = inst
            indexSymbolMap[name] = inst
            indexSymbolMap[inst.symbol.uppercase()] = inst
            symbolExchangeMap["${inst.exch_seg}:${inst.symbol.uppercase()}"] = inst
            symbolExchangeMap["${inst.exch_seg}:$name"] = inst
        }

        // Standard stock tokens (NSE)
        val defaultStocks = listOf(
            Instrument("2885", "RELIANCE-EQ", "RELIANCE", "", "0", "1", "EQ", "NSE", "0.05"),
            Instrument("11536", "TCS-EQ", "TCS", "", "0", "1", "EQ", "NSE", "0.05"),
            Instrument("1594", "INFY-EQ", "INFY", "", "0", "1", "EQ", "NSE", "0.05"),
            Instrument("3045", "SBIN-EQ", "SBIN", "", "0", "1", "EQ", "NSE", "0.05"),
            Instrument("1333", "HDFCBANK-EQ", "HDFCBANK", "", "0", "1", "EQ", "NSE", "0.05"),
            Instrument("4963", "ICICIBANK-EQ", "ICICIBANK", "", "0", "1", "EQ", "NSE", "0.05"),
            Instrument("3499", "TATAMOTORS-EQ", "TATAMOTORS", "", "0", "1", "EQ", "NSE", "0.05"),
            Instrument("3492", "TATASTEEL-EQ", "TATASTEEL", "", "0", "1", "EQ", "NSE", "0.05")
        )
        defaultStocks.forEach { inst ->
            val compKey = "1:${inst.token}"
            instrumentMap[compKey] = inst
            symbolExchangeMap["NSE:${inst.name.uppercase()}"] = inst
            symbolExchangeMap["NSE:${inst.symbol.uppercase()}"] = inst
        }
    }

    private fun registerInstrument(inst: Instrument) {
        val normExch = normalizeExchange(inst.exch_seg)
        val exchType = getExchangeType(normExch)
        val compKey = "$exchType:${inst.token}"
        instrumentMap[compKey] = inst

        val symUpper = inst.symbol.uppercase().trim()
        val nameUpper = inst.name.uppercase().trim()

        if (symUpper.isNotBlank()) {
            symbolExchangeMap["$normExch:$symUpper"] = inst
        }
        if (nameUpper.isNotBlank()) {
            symbolExchangeMap["$normExch:$nameUpper"] = inst
        }

        // Index mapping
        if ((normExch == "NSE" || normExch == "BSE" || normExch == "MCX") &&
            (inst.instrumenttype == "AMXIDX" || inst.instrumenttype == "" || inst.instrumenttype.contains("IDX") || inst.instrumenttype.contains("FUT"))
        ) {
            when {
                nameUpper == "NIFTY" || symUpper == "NIFTY 50" || symUpper == "NIFTY" -> indexSymbolMap["NIFTY"] = inst
                nameUpper == "BANKNIFTY" || symUpper == "NIFTY BANK" || symUpper == "BANKNIFTY" -> indexSymbolMap["BANKNIFTY"] = inst
                nameUpper == "FINNIFTY" || symUpper == "NIFTY FIN SERVICE" || symUpper == "FINNIFTY" -> indexSymbolMap["FINNIFTY"] = inst
                nameUpper == "MIDCPNIFTY" || symUpper.contains("MID SELECT") || symUpper == "MIDCPNIFTY" -> indexSymbolMap["MIDCPNIFTY"] = inst
                nameUpper == "SENSEX" || symUpper == "SENSEX" -> indexSymbolMap["SENSEX"] = inst
                nameUpper == "BANKEX" || symUpper == "BANKEX" -> indexSymbolMap["BANKEX"] = inst
                symUpper.startsWith("CRUDEOILM") -> indexSymbolMap["CRUDEOIL M"] = inst
                symUpper.startsWith("CRUDEOIL") -> indexSymbolMap["CRUDEOIL"] = inst
            }
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
                registerInstrument(inst)
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
            Log.d("InstrumentMaster", "Loading master from local cache (${cacheFile.length()} bytes)")
            try {
                FileInputStream(cacheFile).use { parseInputStream(it) }
                if (instrumentMap.size > 1000) {
                    _isLoaded.value = true
                    Log.d("InstrumentMaster", "Loaded ${instrumentMap.size} instruments from local cache")
                    return@withContext
                } else {
                    throw Exception("Not enough instruments loaded from cache: ${instrumentMap.size}")
                }
            } catch (e: Exception) {
                Log.w("InstrumentMaster", "Failed to parse cached master file, re-downloading", e)
                cacheFile.delete()
                instrumentMap.clear()
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
                    val expectedLength = body.contentLength()
                    
                    if (cacheFile != null) {
                        body.byteStream().use { input ->
                            FileOutputStream(cacheFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        val actualLength = cacheFile.length()
                        if (expectedLength > 0 && actualLength != expectedLength) {
                            throw Exception("Truncated download: expected $expectedLength bytes, got $actualLength bytes")
                        }
                        
                        FileInputStream(cacheFile).use { parseInputStream(it) }
                    } else {
                        body.byteStream().use { parseInputStream(it) }
                    }

                    if (instrumentMap.size > 1000) {
                        _isLoaded.value = true
                        success = true
                        Log.d("InstrumentMaster", "Loaded ${instrumentMap.size} instruments successfully")
                    } else {
                        throw Exception("Parsing completed but not enough instruments: ${instrumentMap.size}")
                    }
                }
            } catch (e: Exception) {
                Log.e("InstrumentMaster", "Attempt $attempts failed to download/parse master: ${e.localizedMessage}")
                instrumentMap.clear()
                if (attempts < maxAttempts) {
                    kotlinx.coroutines.delay(2000L * attempts)
                }
            }
        }

        // If network downloads failed but an older cache file exists, fallback to reading older cache
        if (!success && cacheFile != null && cacheFile.exists() && cacheFile.length() > 0) {
            try {
                Log.d("InstrumentMaster", "Falling back to existing cached master file after network failure")
                FileInputStream(cacheFile).use { parseInputStream(it) }
                if (instrumentMap.size > 1000) {
                    _isLoaded.value = true
                    Log.d("InstrumentMaster", "Loaded ${instrumentMap.size} instruments from fallback cache")
                }
            } catch (e: Exception) {
                Log.e("InstrumentMaster", "Failed reading fallback cache file", e)
            }
        }
    }

    fun getInstrumentByToken(token: String, exchangeType: Int): Instrument? {
        val compKey = "$exchangeType:${token.trim()}"
        return instrumentMap[compKey]
    }

    fun getInstrument(exchange: String, token: String): Instrument? {
        val exchType = getExchangeType(exchange)
        return getInstrumentByToken(token, exchType)
    }

    fun getOptionInstruments(name: String, expiry: String): List<Instrument> {
        val uppercaseName = name.uppercase().trim()
        val nfoName = when (uppercaseName) {
            "NIFTY 50", "NIFTY50" -> "NIFTY"
            "BANK NIFTY", "NIFTY BANK" -> "BANKNIFTY"
            "FIN NIFTY", "NIFTY FIN SERVICE" -> "FINNIFTY"
            "MIDCAP NIFTY", "MIDCP NIFTY" -> "MIDCPNIFTY"
            else -> uppercaseName
        }
        
        val exchSeg = when (uppercaseName) {
            "SENSEX", "BANKEX" -> "BFO"
            "CRUDEOIL", "CRUDEOIL M" -> "MCX"
            else -> "NFO"
        }
        
        return instrumentMap.values.filter {
            normalizeExchange(it.exch_seg) == exchSeg &&
            (it.name.equals(nfoName, ignoreCase = true) || it.symbol.startsWith(nfoName, ignoreCase = true)) &&
            (expiry.isBlank() || it.expiry.equals(expiry, ignoreCase = true) || it.expiry.replace("-", "").equals(expiry.replace("-", ""), ignoreCase = true))
        }
    }

    fun resolveOptionInstrument(
        underlying: String,
        expiry: String,
        strike: Double,
        optionType: String
    ): Instrument? {
        val cleanUnderlying = when (underlying.uppercase().trim()) {
            "NIFTY 50", "NIFTY50" -> "NIFTY"
            "BANK NIFTY", "NIFTY BANK" -> "BANKNIFTY"
            "FIN NIFTY", "NIFTY FIN SERVICE" -> "FINNIFTY"
            "MIDCAP NIFTY", "MIDCP NIFTY" -> "MIDCPNIFTY"
            else -> underlying.uppercase().trim()
        }
        val cleanType = optionType.uppercase().trim() // CE or PE
        val exchSeg = when (cleanUnderlying) {
            "SENSEX", "BANKEX" -> "BFO"
            "CRUDEOIL", "CRUDEOIL M" -> "MCX"
            else -> "NFO"
        }

        val matchingOptions = instrumentMap.values.filter { inst ->
            normalizeExchange(inst.exch_seg) == exchSeg &&
            (inst.name.equals(cleanUnderlying, ignoreCase = true) || inst.symbol.startsWith(cleanUnderlying, ignoreCase = true)) &&
            (inst.symbol.endsWith(cleanType, ignoreCase = true) || inst.symbol.contains(cleanType, ignoreCase = true))
        }

        // Match strike & expiry
        return matchingOptions.find { inst ->
            val instStrike = inst.strike.toDoubleOrNull() ?: 0.0
            val strikeMatches = kotlin.math.abs(instStrike - strike) < 1.0 ||
                                kotlin.math.abs(instStrike - (strike * 100.0)) < 1.0 ||
                                kotlin.math.abs((instStrike / 100.0) - strike) < 1.0

            val expiryMatches = expiry.isBlank() ||
                                inst.expiry.equals(expiry, ignoreCase = true) ||
                                inst.expiry.replace("-", "").equals(expiry.replace("-", ""), ignoreCase = true)

            strikeMatches && expiryMatches
        }
    }

    fun getOptionExpiries(name: String): List<String> {
        val uppercaseName = name.uppercase().trim()
        val nfoName = when (uppercaseName) {
            "NIFTY 50", "NIFTY50" -> "NIFTY"
            "BANK NIFTY", "NIFTY BANK" -> "BANKNIFTY"
            "FIN NIFTY", "NIFTY FIN SERVICE" -> "FINNIFTY"
            "MIDCAP NIFTY", "MIDCP NIFTY" -> "MIDCPNIFTY"
            else -> uppercaseName
        }
        
        val exchSeg = when (uppercaseName) {
            "SENSEX", "BANKEX" -> "BFO"
            "CRUDEOIL", "CRUDEOIL M" -> "MCX"
            else -> "NFO"
        }
        
        return instrumentMap.values.filter {
            normalizeExchange(it.exch_seg) == exchSeg &&
            (it.name.equals(nfoName, ignoreCase = true) || it.symbol.startsWith(nfoName, ignoreCase = true)) &&
            it.expiry.isNotBlank()
        }.map { it.expiry }.distinct().sorted()
    }

    fun resolveIndexToken(indexName: String): Instrument? {
        val cleanName = when (indexName.uppercase().trim()) {
            "NIFTY 50", "NIFTY50" -> "NIFTY"
            "BANK NIFTY", "NIFTY BANK" -> "BANKNIFTY"
            "FIN NIFTY", "NIFTY FIN SERVICE" -> "FINNIFTY"
            "MIDCAP NIFTY", "MIDCP NIFTY" -> "MIDCPNIFTY"
            else -> indexName.uppercase().trim()
        }
        return indexSymbolMap[cleanName] ?: hardcodedIndices[cleanName]
    }

    fun resolveAngelToken(symbol: String, exchange: String = "NSE"): String? {
        val uppercaseSymbol = symbol.uppercase().trim()
        val normExch = normalizeExchange(exchange)

        // 1. Check numeric token passed directly
        if (uppercaseSymbol.all { it.isDigit() }) return uppercaseSymbol

        // 2. Check Index mapping
        val cleanIndex = when (uppercaseSymbol) {
            "NIFTY 50", "NIFTY50" -> "NIFTY"
            "BANK NIFTY", "NIFTY BANK" -> "BANKNIFTY"
            "FIN NIFTY", "NIFTY FIN SERVICE" -> "FINNIFTY"
            "MIDCAP NIFTY", "MIDCP NIFTY" -> "MIDCPNIFTY"
            else -> uppercaseSymbol
        }
        val indexInst = indexSymbolMap[cleanIndex] ?: hardcodedIndices[cleanIndex]
        if (indexInst != null && (exchange.isBlank() || normalizeExchange(indexInst.exch_seg) == normExch)) {
            return indexInst.token
        }

        // 3. Check Symbol + Exchange Map
        val directMatch = symbolExchangeMap["$normExch:$uppercaseSymbol"]
            ?: symbolExchangeMap["$normExch:${uppercaseSymbol}-EQ"]
            ?: symbolExchangeMap["$normExch:${uppercaseSymbol}-FUT"]
        if (directMatch != null) return directMatch.token

        // 4. Scan instrumentMap values
        val match = instrumentMap.values.find {
            (exchange.isBlank() || normalizeExchange(it.exch_seg) == normExch) &&
            (it.symbol.equals(uppercaseSymbol, ignoreCase = true) ||
             it.name.equals(uppercaseSymbol, ignoreCase = true) ||
             it.symbol.equals("${uppercaseSymbol}-EQ", ignoreCase = true))
        }
        return match?.token ?: indexInst?.token
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

        if (uppercaseSymbol.all { it.isDigit() }) return uppercaseSymbol

        val match = instrumentMap.values.find {
            it.symbol.equals(uppercaseSymbol, ignoreCase = true) || it.name.equals(uppercaseSymbol, ignoreCase = true)
        }
        return match?.token
    }
}

