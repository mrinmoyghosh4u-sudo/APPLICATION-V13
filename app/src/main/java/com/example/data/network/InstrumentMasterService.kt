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
    init {
        instance = this
    }

    fun getLotSizeForSymbol(symbol: String): Int {
        val upper = symbol.uppercase().trim()
        val inst = instrumentMap.values.find { it.symbol.equals(upper, ignoreCase = true) || it.name.equals(upper, ignoreCase = true) }
        return inst?.lotsize?.toIntOrNull() ?: 0
    }

    companion object {
        @Volatile
        var instance: InstrumentMasterService? = null
            private set
        fun normalizeExchange(exchSeg: String): String {
            return when (exchSeg.trim().lowercase()) {
                "nse", "nse_cm", "nse-cm", "nse_eq" -> "NSE"
                "nfo", "nse_fo", "nse-fo", "nse_fno" -> "NFO"
                "bse", "bse_cm", "bse-cm", "bse_eq" -> "BSE"
                "bfo", "bse_fo", "bse-fo", "bse_fno" -> "BFO"
                "mcx", "mcx_fo", "mcx-fo", "mcx_cm", "mcx_comm", "nco", "mcx_opt", "mcx_fut" -> "MCX"
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

    private val _isLoaded = MutableStateFlow(false)
    val isLoadedFlow = _isLoaded.asStateFlow()
    val isLoaded: Boolean get() = _isLoaded.value

    suspend fun ensureLoaded() {
        if (_isLoaded.value) return
        withContext(Dispatchers.IO) {
            if (!_isLoaded.value) {
                loadMaster()
            }
        }
    }

    init {
        // No hardcoded stock/option tokens populated here.
        // Instrument Master is the single source of truth.
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
            if (symUpper.endsWith("-EQ")) {
                val clean = symUpper.removeSuffix("-EQ").trim()
                if (clean.isNotBlank()) {
                    symbolExchangeMap["$normExch:$clean"] = inst
                }
            }
        }
        if (nameUpper.isNotBlank()) {
            symbolExchangeMap["$normExch:$nameUpper"] = inst
        }

        // Index mapping
        if (normExch == "NSE" || normExch == "BSE" || normExch == "MCX") {
            val isNotOption = !symUpper.contains(" CE") && !symUpper.contains(" PE") && 
                              !symUpper.endsWith("CE") && !symUpper.endsWith("PE") && 
                              !inst.symbol.endsWith("CE") && !inst.symbol.endsWith("PE") &&
                              !inst.instrumenttype.contains("OPT", ignoreCase = true)
            
            val isNotFuture = !symUpper.contains(" FUT") && !symUpper.endsWith("FUT") && 
                              !inst.instrumenttype.contains("FUT", ignoreCase = true)

            if (isNotOption) {
                when {
                    normExch == "NSE" && isNotFuture && (symUpper == "NIFTY 50" || symUpper == "NIFTY" || inst.token == "99926000" || inst.token == "26000") -> indexSymbolMap["NIFTY"] = inst
                    normExch == "NSE" && isNotFuture && (symUpper == "NIFTY BANK" || symUpper == "BANKNIFTY" || inst.token == "99926009" || inst.token == "26009") -> indexSymbolMap["BANKNIFTY"] = inst
                    normExch == "NSE" && isNotFuture && (symUpper == "NIFTY FIN SERVICE" || symUpper == "FINNIFTY" || inst.token == "99926037" || inst.token == "26037") -> indexSymbolMap["FINNIFTY"] = inst
                    normExch == "NSE" && isNotFuture && (symUpper == "MIDCPNIFTY" || symUpper.contains("MID SELECT") || inst.token == "99926074" || inst.token == "26074") -> indexSymbolMap["MIDCPNIFTY"] = inst
                    normExch == "BSE" && isNotFuture && (symUpper == "SENSEX" || symUpper == "BSESN" || nameUpper == "SENSEX" || inst.token == "99919000" || inst.token == "1") -> {
                        val current = indexSymbolMap["SENSEX"]
                        if (current == null || inst.token == "99919000" || inst.instrumenttype.contains("IDX", ignoreCase = true)) {
                            indexSymbolMap["SENSEX"] = inst
                        }
                    }
                    normExch == "BSE" && isNotFuture && (symUpper == "BANKEX" || inst.token == "99919012") -> {
                        val current = indexSymbolMap["BANKEX"]
                        if (current == null || inst.token == "99919012" || inst.instrumenttype.contains("IDX", ignoreCase = true)) {
                            indexSymbolMap["BANKEX"] = inst
                        }
                    }
                    normExch == "MCX" && (nameUpper == "CRUDEOILM" || symUpper.startsWith("CRUDEOILM")) && !inst.instrumenttype.contains("OPT", ignoreCase = true) -> {
                        val current = indexSymbolMap["CRUDEOIL M"]
                        if (current == null || isEarlierActiveExpiry(inst.expiry, current.expiry)) {
                            indexSymbolMap["CRUDEOIL M"] = inst
                        }
                    }
                    normExch == "MCX" && (nameUpper == "CRUDEOIL" || symUpper.startsWith("CRUDEOIL")) && !symUpper.startsWith("CRUDEOILM") && !inst.instrumenttype.contains("OPT", ignoreCase = true) -> {
                        val current = indexSymbolMap["CRUDEOIL"]
                        if (current == null || isEarlierActiveExpiry(inst.expiry, current.expiry)) {
                            indexSymbolMap["CRUDEOIL"] = inst
                        }
                    }
                }
            }
        }
    }

    private fun isEarlierActiveExpiry(newExpiry: String, currentExpiry: String): Boolean {
        if (currentExpiry.isBlank()) return true
        if (newExpiry.isBlank()) return false
        val newTime = parseExpiryDate(newExpiry)
        val curTime = parseExpiryDate(currentExpiry)
        val now = System.currentTimeMillis() - 86400000L // allow today
        if (newTime < now && curTime >= now) return false
        if (newTime >= now && curTime < now) return true
        return newTime < curTime
    }

    private fun parseExpiryDate(expiryStr: String): Long {
        if (expiryStr.isBlank()) return Long.MAX_VALUE
        val clean = expiryStr.trim().uppercase()
        val formats = listOf(
            java.text.SimpleDateFormat("ddMMMyyyy", java.util.Locale.ENGLISH),
            java.text.SimpleDateFormat("dd-MMM-yyyy", java.util.Locale.ENGLISH),
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH),
            java.text.SimpleDateFormat("ddMMMyy", java.util.Locale.ENGLISH)
        )
        for (f in formats) {
            try {
                val d = f.parse(clean)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return Long.MAX_VALUE
    }

    private fun readNextStringSafe(reader: JsonReader): String {
        return try {
            when (reader.peek()) {
                android.util.JsonToken.NULL -> {
                    reader.nextNull()
                    ""
                }
                android.util.JsonToken.STRING -> reader.nextString() ?: ""
                android.util.JsonToken.NUMBER -> {
                    try {
                        reader.nextString() ?: ""
                    } catch (e: Exception) {
                        try {
                            reader.nextDouble().toString()
                        } catch (e2: Exception) {
                            ""
                        }
                    }
                }
                android.util.JsonToken.BOOLEAN -> reader.nextBoolean().toString()
                else -> {
                    reader.skipValue()
                    ""
                }
            }
        } catch (e: Exception) {
            ""
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
                    "token" -> token = readNextStringSafe(reader)
                    "symbol" -> symbol = readNextStringSafe(reader)
                    "name" -> name = readNextStringSafe(reader)
                    "expiry" -> expiry = readNextStringSafe(reader)
                    "strike" -> strike = readNextStringSafe(reader)
                    "lotsize" -> lotsize = readNextStringSafe(reader)
                    "instrumenttype" -> instType = readNextStringSafe(reader)
                    "exch_seg" -> exch = readNextStringSafe(reader)
                    "tick_size" -> tickSize = readNextStringSafe(reader)
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
                        Log.d("InstrumentMaster", "[INSTRUMENT_MASTER_READY] count=${instrumentMap.size}")
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
                    Log.d("InstrumentMaster", "[INSTRUMENT_MASTER_READY] fallback_count=${instrumentMap.size}")
                }
            } catch (e: Exception) {
                Log.e("InstrumentMaster", "Failed reading fallback cache file", e)
            }
        }
    }

    
    fun searchInstruments(query: String): List<Instrument> {
        val q = query.trim().uppercase()
        if (q.isBlank()) return emptyList()
        val tokens = q.split(" ").filter { it.isNotBlank() }
        
        return instrumentMap.values.filter { inst ->
            val sym = inst.symbol.uppercase()
            val name = inst.name.uppercase()
            tokens.all { t -> sym.contains(t) || name.contains(t) }
        }.take(30)
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

        val cleanExpiry = expiry.replace("-", "").replace(" ", "").replace("/", "").uppercase()

        val exactMatches = instrumentMap.values.filter {
            normalizeExchange(it.exch_seg) == exchSeg &&
            (it.name.equals(nfoName, ignoreCase = true) || it.symbol.startsWith(nfoName, ignoreCase = true)) &&
            (cleanExpiry.isBlank() || it.expiry.replace("-", "").replace(" ", "").replace("/", "").uppercase().equals(cleanExpiry, ignoreCase = true))
        }

        if (exactMatches.isNotEmpty()) return exactMatches

        return emptyList()
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

        // Match exact strike & expiry from Instrument Master
        return matchingOptions.find { inst ->
            val instStrikeRaw = inst.strike.toDoubleOrNull() ?: 0.0
            val isCommodity = exchSeg == "MCX" || normalizeExchange(inst.exch_seg) == "MCX"
            // MCX commodity contracts (e.g. CRUDEOIL) already store strikes in rupees and must not be divided.
            // NSE/BFO index contracts in Angel One master store strikes in paise (e.g. 2485000 for 24850.0).
            val instStrike = if (instStrikeRaw > 100000) {
                instStrikeRaw / 100.0
            } else if (instStrikeRaw > 10000 && !isCommodity) {
                instStrikeRaw / 100.0
            } else {
                instStrikeRaw
            }

            val strikeMatches = kotlin.math.abs(instStrike - strike) < 0.01

            val angelTargetExpiry = com.example.util.OptionExpiryUtil.formatForAngel(expiry)
            val expiryMatches = expiry.isBlank() ||
                                inst.expiry.equals(expiry, ignoreCase = true) ||
                                inst.expiry.equals(angelTargetExpiry, ignoreCase = true) ||
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
        val upper = indexName.uppercase().trim().removePrefix("NSE:").removePrefix("BSE:").removePrefix("MCX:")
        val cleanName = when {
            upper == "NIFTY 50" || upper == "NIFTY50" || upper == "NIFTY" -> "NIFTY"
            upper == "BANK NIFTY" || upper == "NIFTY BANK" || upper == "BANKNIFTY" -> "BANKNIFTY"
            upper == "FIN NIFTY" || upper == "NIFTY FIN SERVICE" || upper == "FINNIFTY" -> "FINNIFTY"
            upper.contains("MID SELECT") || upper == "MIDCAP NIFTY" || upper == "MIDCP NIFTY" || upper == "MIDCPNIFTY" -> "MIDCPNIFTY"
            upper == "SENSEX" || upper == "BSE SENSEX" -> "SENSEX"
            upper == "BANKEX" || upper == "BSE BANKEX" -> "BANKEX"
            upper == "CRUDEOIL M" || upper == "CRUDEOILM" || upper == "CRUDE OIL M" || upper == "CRUDEOIL MINI" -> "CRUDEOIL M"
            upper == "CRUDEOIL" || upper == "CRUDE OIL" || upper.startsWith("CRUDEOIL") -> "CRUDEOIL"
            else -> upper
        }
        return indexSymbolMap[cleanName]
    }

    fun getActiveContract(symbol: String): Instrument? {
        val clean = symbol.uppercase().trim()
        return resolveIndexToken(clean) ?: instrumentMap.values.find { it.symbol.equals(clean, ignoreCase = true) || it.name.equals(clean, ignoreCase = true) }
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
        val indexInst = indexSymbolMap[cleanIndex]
        if (indexInst != null && (exchange.isBlank() || normalizeExchange(indexInst.exch_seg) == normExch)) {
            Log.d("InstrumentMaster", "[TOKEN_RESOLVED] index=$symbol exch=$exchange instrument_id=${indexInst.token}")
            return indexInst.token
        }

        // 3. Check Symbol + Exchange Map
        val directMatch = symbolExchangeMap["$normExch:$uppercaseSymbol"]
            ?: symbolExchangeMap["$normExch:${uppercaseSymbol}-EQ"]
            ?: symbolExchangeMap["$normExch:${uppercaseSymbol}-FUT"]
        if (directMatch != null) {
            Log.d("InstrumentMaster", "[TOKEN_RESOLVED] symbol=$symbol exch=$exchange instrument_id=${directMatch.token}")
            return directMatch.token
        }

        // 4. Scan instrumentMap values
        val match = instrumentMap.values.find {
            (exchange.isBlank() || normalizeExchange(it.exch_seg) == normExch) &&
            (it.symbol.equals(uppercaseSymbol, ignoreCase = true) ||
             it.name.equals(uppercaseSymbol, ignoreCase = true) ||
             it.symbol.equals("${uppercaseSymbol}-EQ", ignoreCase = true))
        }
        val resolved = match?.token ?: indexInst?.token
        if (resolved != null) {
            Log.d("InstrumentMaster", "[TOKEN_RESOLVED] symbol=$symbol exch=$exchange instrument_id=$resolved")
        }
        return resolved
    }

    fun resolveUpstoxInstrumentKey(symbol: String, exchange: String = "NSE"): String {
        return UpstoxSymbolMapper.toUpstoxInstrumentKey(symbol, exchange)
    }

    fun resolveDhanSecurityId(symbol: String, exchange: String = "NSE"): String? {
        val uppercaseSymbol = symbol.uppercase().trim()
        
        val cleanSymbol = uppercaseSymbol
            .removePrefix("NSE:")
            .removePrefix("BSE:")
            .removePrefix("MCX:")
            .removePrefix("NFO:")
            .removePrefix("BFO:")
            .trim()
           
        when (cleanSymbol) {
            "NIFTY", "NIFTY 50", "NIFTY50", "13" -> return "13"
            "BANKNIFTY", "25" -> return "25"
            "FINNIFTY", "27" -> return "27"
            "MIDCPNIFTY", "31" -> return "31"
            "SENSEX", "51" -> return "51"
            "BANKEX", "17" -> return "17"
            "CRUDEOIL" -> return "418042"
            "CRUDEOIL M", "CRUDEOILM" -> return "418043"
        }

        if (cleanSymbol.all { it.isDigit() }) return cleanSymbol

        // Security ID Isolation: NEVER fall back to Angel One's match?.token from OpenAPIScripMaster.json.
        // Return null if no verified Dhan security ID mapping is found to prevent placing orders on unintended instruments.
        Log.w("InstrumentMaster", "[DHAN_SECURITY_ID_NOT_FOUND] No verified Dhan security ID mapping found for symbol: $symbol, exchange: $exchange")
        return null
    }
}

