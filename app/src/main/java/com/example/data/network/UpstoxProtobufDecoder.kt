package com.example.data.network

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class UpstoxDecodedFeed(
    val instrumentKey: String,
    val ltp: Double = 0.0,
    val open: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val close: Double = 0.0,
    val volume: Long = 0L,
    val ltq: Long = 0L,
    val oi: Double = 0.0,
    val timestamp: Long = 0L,
    val bidPrice: Double = 0.0,
    val bidQty: Long = 0L,
    val askPrice: Double = 0.0,
    val askQty: Long = 0L,
    val iv: Double = 0.0,
    val delta: Double = 0.0,
    val theta: Double = 0.0,
    val gamma: Double = 0.0,
    val vega: Double = 0.0,
    val pcr: Double = 0.0
)

data class UpstoxDecodedFeedResponse(
    val feedType: Int = 0,
    val currentTs: Long = 0L,
    val feeds: Map<String, UpstoxDecodedFeed> = emptyMap()
)

/**
 * Zero-dependency, ultra-fast Protobuf wire-format binary decoder for Upstox V3 Market Data Feed.
 * Conforms to Upstox MarketDataFeed.proto schema.
 */
object UpstoxProtobufDecoder {
    private const val TAG = "UpstoxProtoDecoder"

    fun decode(bytes: ByteArray): UpstoxDecodedFeedResponse {
        return try {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            var feedType = 0
            var currentTs = 0L
            val feeds = mutableMapOf<String, UpstoxDecodedFeed>()

            while (buffer.hasRemaining()) {
                val tag = readVarint(buffer)
                val fieldNum = (tag ushr 3).toInt()
                val wireType = (tag and 0x07).toInt()

                when (fieldNum) {
                    1 -> { // Type feedType
                        if (wireType == 0) {
                            feedType = readVarint(buffer).toInt()
                        } else {
                            skipField(buffer, wireType)
                        }
                    }
                    2 -> { // map<string, Feed> feeds
                        if (wireType == 2) {
                            val len = readVarint(buffer).toInt()
                            if (len in 1..buffer.remaining()) {
                                val subBuf = sliceBuffer(buffer, len)
                                parseFeedMapEntry(subBuf)?.let { (key, feed) ->
                                    feeds[key] = feed
                                }
                            }
                        } else {
                            skipField(buffer, wireType)
                        }
                    }
                    3 -> { // int64 currentTs
                        if (wireType == 0) {
                            currentTs = readVarint(buffer)
                        } else {
                            skipField(buffer, wireType)
                        }
                    }
                    else -> skipField(buffer, wireType)
                }
            }

            UpstoxDecodedFeedResponse(
                feedType = feedType,
                currentTs = currentTs,
                feeds = feeds
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode Upstox Protobuf binary packet: ${e.message}")
            UpstoxDecodedFeedResponse()
        }
    }

    private fun parseFeedMapEntry(buffer: ByteBuffer): Pair<String, UpstoxDecodedFeed>? {
        var key = ""
        var feed: UpstoxDecodedFeed? = null

        while (buffer.hasRemaining()) {
            val tag = readVarint(buffer)
            val fieldNum = (tag ushr 3).toInt()
            val wireType = (tag and 0x07).toInt()

            when (fieldNum) {
                1 -> { // string key
                    val len = readVarint(buffer).toInt()
                    if (len in 1..buffer.remaining()) {
                        val bytes = ByteArray(len)
                        buffer.get(bytes)
                        key = String(bytes, Charsets.UTF_8)
                    }
                }
                2 -> { // Feed value
                    val len = readVarint(buffer).toInt()
                    if (len in 1..buffer.remaining()) {
                        val subBuf = sliceBuffer(buffer, len)
                        feed = parseFeed(subBuf, key)
                    }
                }
                else -> skipField(buffer, wireType)
            }
        }

        return if (key.isNotBlank() && feed != null) {
            Pair(key, feed)
        } else null
    }

    private fun parseFeed(buffer: ByteBuffer, key: String): UpstoxDecodedFeed {
        var ltp = 0.0
        var open = 0.0
        var high = 0.0
        var low = 0.0
        var close = 0.0
        var volume = 0L
        var ltq = 0L
        var oi = 0.0
        var timestamp = 0L
        var bidPrice = 0.0
        var bidQty = 0L
        var askPrice = 0.0
        var askQty = 0L
        var iv = 0.0
        var delta = 0.0
        var theta = 0.0
        var gamma = 0.0
        var vega = 0.0
        var pcr = 0.0

        while (buffer.hasRemaining()) {
            val tag = readVarint(buffer)
            val fieldNum = (tag ushr 3).toInt()
            val wireType = (tag and 0x07).toInt()

            when (fieldNum) {
                1 -> { // LTPC ltpc
                    val len = readVarint(buffer).toInt()
                    if (len in 1..buffer.remaining()) {
                        val subBuf = sliceBuffer(buffer, len)
                        val (pLtp, pTs, pClose, pLtq) = parseLtpc(subBuf)
                        if (pLtp > 0.0) ltp = pLtp
                        if (pTs > 0L) timestamp = pTs
                        if (pClose > 0.0) close = pClose
                        if (pLtq > 0L) ltq = pLtq
                    }
                }
                2 -> { // FullFeed fullFeed
                    val len = readVarint(buffer).toInt()
                    if (len in 1..buffer.remaining()) {
                        val subBuf = sliceBuffer(buffer, len)
                        while (subBuf.hasRemaining()) {
                            val subTag = readVarint(subBuf)
                            val subField = (subTag ushr 3).toInt()
                            val subWire = (subTag and 0x07).toInt()

                            when (subField) {
                                1 -> { // MarketFullFeed marketFF
                                    val mLen = readVarint(subBuf).toInt()
                                    if (mLen in 1..subBuf.remaining()) {
                                        val mBuf = sliceBuffer(subBuf, mLen)
                                        while (mBuf.hasRemaining()) {
                                            val mTag = readVarint(mBuf)
                                            val mField = (mTag ushr 3).toInt()
                                            val mWire = (mTag and 0x07).toInt()
                                            when (mField) {
                                                1 -> { // LTPC
                                                    val lLen = readVarint(mBuf).toInt()
                                                    if (lLen in 1..mBuf.remaining()) {
                                                        val (pLtp, pTs, pClose, pLtq) = parseLtpc(sliceBuffer(mBuf, lLen))
                                                        if (pLtp > 0.0) ltp = pLtp
                                                        if (pTs > 0L) timestamp = pTs
                                                        if (pClose > 0.0) close = pClose
                                                        if (pLtq > 0L) ltq = pLtq
                                                    }
                                                }
                                                2 -> { // MarketLevel marketLevel
                                                    val lvlLen = readVarint(mBuf).toInt()
                                                    if (lvlLen in 1..mBuf.remaining()) {
                                                        val lvlBuf = sliceBuffer(mBuf, lvlLen)
                                                        val (bPrice, bQty, aPrice, aQty) = parseMarketLevel(lvlBuf)
                                                        if (bPrice > 0.0) bidPrice = bPrice
                                                        if (bQty > 0L) bidQty = bQty
                                                        if (aPrice > 0.0) askPrice = aPrice
                                                        if (aQty > 0L) askQty = aQty
                                                    }
                                                }
                                                3 -> { // MarketOHLC eFeedDetails
                                                    val ohlcLen = readVarint(mBuf).toInt()
                                                    if (ohlcLen in 1..mBuf.remaining()) {
                                                        val ohlcBuf = sliceBuffer(mBuf, ohlcLen)
                                                        val (mOpen, mHigh, mLow, mClose, mVol, mOi, mTs) = parseMarketOhlc(ohlcBuf)
                                                        if (mOpen > 0.0) open = mOpen
                                                        if (mHigh > 0.0) high = mHigh
                                                        if (mLow > 0.0) low = mLow
                                                        if (mClose > 0.0) close = mClose
                                                        if (mVol > 0L) volume = mVol
                                                        if (mOi > 0.0) oi = mOi
                                                        if (mTs > 0L) timestamp = mTs
                                                    }
                                                }
                                                else -> skipField(mBuf, mWire)
                                            }
                                        }
                                    }
                                }
                                2 -> { // IndexFullFeed indexFF
                                    val iLen = readVarint(subBuf).toInt()
                                    if (iLen in 1..subBuf.remaining()) {
                                        val iBuf = sliceBuffer(subBuf, iLen)
                                        while (iBuf.hasRemaining()) {
                                            val iTag = readVarint(iBuf)
                                            val iField = (iTag ushr 3).toInt()
                                            val iWire = (iTag and 0x07).toInt()
                                            when (iField) {
                                                1 -> { // LTPC
                                                    val lLen = readVarint(iBuf).toInt()
                                                    if (lLen in 1..iBuf.remaining()) {
                                                        val (pLtp, pTs, pClose, pLtq) = parseLtpc(sliceBuffer(iBuf, lLen))
                                                        if (pLtp > 0.0) ltp = pLtp
                                                        if (pTs > 0L) timestamp = pTs
                                                        if (pClose > 0.0) close = pClose
                                                        if (pLtq > 0L) ltq = pLtq
                                                    }
                                                }
                                                2 -> { // IndexOHLC eFeedDetails
                                                    val ohlcLen = readVarint(iBuf).toInt()
                                                    if (ohlcLen in 1..iBuf.remaining()) {
                                                        val ohlcBuf = sliceBuffer(iBuf, ohlcLen)
                                                        val (iOpen, iHigh, iLow, iClose, iTs) = parseIndexOhlc(ohlcBuf)
                                                        if (iOpen > 0.0) open = iOpen
                                                        if (iHigh > 0.0) high = iHigh
                                                        if (iLow > 0.0) low = iLow
                                                        if (iClose > 0.0) close = iClose
                                                        if (iTs > 0L) timestamp = iTs
                                                    }
                                                }
                                                else -> skipField(iBuf, iWire)
                                            }
                                        }
                                    }
                                }
                                else -> skipField(subBuf, subWire)
                            }
                        }
                    }
                }
                3 -> { // OptionGreeks optionGreeks
                    val len = readVarint(buffer).toInt()
                    if (len in 1..buffer.remaining()) {
                        val subBuf = sliceBuffer(buffer, len)
                        while (subBuf.hasRemaining()) {
                            val gTag = readVarint(subBuf)
                            val gField = (gTag ushr 3).toInt()
                            val gWire = (gTag and 0x07).toInt()
                            when (gField) {
                                3 -> iv = readDoubleOrFloat(subBuf, gWire)
                                4 -> delta = readDoubleOrFloat(subBuf, gWire)
                                5 -> theta = readDoubleOrFloat(subBuf, gWire)
                                6 -> gamma = readDoubleOrFloat(subBuf, gWire)
                                7 -> vega = readDoubleOrFloat(subBuf, gWire)
                                8 -> pcr = readDoubleOrFloat(subBuf, gWire)
                                9 -> oi = readDoubleOrFloat(subBuf, gWire)
                                else -> skipField(subBuf, gWire)
                            }
                        }
                    }
                }
                else -> skipField(buffer, wireType)
            }
        }

        return UpstoxDecodedFeed(
            instrumentKey = key,
            ltp = ltp,
            open = open,
            high = high,
            low = low,
            close = close,
            volume = volume,
            ltq = ltq,
            oi = oi,
            timestamp = timestamp,
            bidPrice = bidPrice,
            bidQty = bidQty,
            askPrice = askPrice,
            askQty = askQty,
            iv = iv,
            delta = delta,
            theta = theta,
            gamma = gamma,
            vega = vega,
            pcr = pcr
        )
    }

    private data class LtpcResult(val ltp: Double, val timestamp: Long, val close: Double, val ltq: Long)

    private fun parseLtpc(buffer: ByteBuffer): LtpcResult {
        var ltp = 0.0
        var ts = 0L
        var close = 0.0
        var ltq = 0L

        while (buffer.hasRemaining()) {
            val tag = readVarint(buffer)
            val field = (tag ushr 3).toInt()
            val wire = (tag and 0x07).toInt()
            when (field) {
                1 -> ltp = readDoubleOrFloat(buffer, wire) // double ltp
                2 -> ts = readVarint(buffer)  // int64 ltt
                3 -> ltq = readVarint(buffer) // int64 ltq
                4 -> close = readDoubleOrFloat(buffer, wire) // double cp
                else -> skipField(buffer, wire)
            }
        }
        return LtpcResult(ltp, ts, close, ltq)
    }

    private data class MarketLevelResult(val bidPrice: Double, val bidQty: Long, val askPrice: Double, val askQty: Long)

    private fun parseMarketLevel(buffer: ByteBuffer): MarketLevelResult {
        var bPrice = 0.0
        var bQty = 0L
        var aPrice = 0.0
        var aQty = 0L

        while (buffer.hasRemaining()) {
            val tag = readVarint(buffer)
            val field = (tag ushr 3).toInt()
            val wire = (tag and 0x07).toInt()
            when (field) {
                1 -> { // repeated Quote bidAskQuote
                    val len = readVarint(buffer).toInt()
                    if (len in 1..buffer.remaining()) {
                        val qBuf = sliceBuffer(buffer, len)
                        while (qBuf.hasRemaining()) {
                            val qTag = readVarint(qBuf)
                            val qField = (qTag ushr 3).toInt()
                            val qWire = (qTag and 0x07).toInt()
                            when (qField) {
                                1 -> if (bQty == 0L) bQty = readVarint(qBuf) else readVarint(qBuf)
                                2 -> if (aQty == 0L) aQty = readVarint(qBuf) else readVarint(qBuf)
                                3 -> if (bPrice == 0.0) bPrice = readDoubleOrFloat(qBuf, qWire) else readDoubleOrFloat(qBuf, qWire)
                                4 -> if (aPrice == 0.0) aPrice = readDoubleOrFloat(qBuf, qWire) else readDoubleOrFloat(qBuf, qWire)
                                else -> skipField(qBuf, qWire)
                            }
                        }
                    }
                }
                else -> skipField(buffer, wire)
            }
        }
        return MarketLevelResult(bPrice, bQty, aPrice, aQty)
    }

    private data class MarketOhlcResult(val open: Double, val high: Double, val low: Double, val close: Double, val volume: Long, val oi: Double, val ts: Long)

    private fun parseMarketOhlc(buffer: ByteBuffer): MarketOhlcResult {
        var open = 0.0
        var high = 0.0
        var low = 0.0
        var close = 0.0
        var volume = 0L
        var oi = 0.0
        var ts = 0L

        while (buffer.hasRemaining()) {
            val tag = readVarint(buffer)
            val field = (tag ushr 3).toInt()
            val wire = (tag and 0x07).toInt()
            when (field) {
                1 -> open = readDoubleOrFloat(buffer, wire)
                2 -> high = readDoubleOrFloat(buffer, wire)
                3 -> low = readDoubleOrFloat(buffer, wire)
                4 -> close = readDoubleOrFloat(buffer, wire)
                5 -> readVarint(buffer) // totalBuyQty
                6 -> readVarint(buffer) // totalSellQty
                7 -> readDoubleOrFloat(buffer, wire) // lowerCircuit
                8 -> readDoubleOrFloat(buffer, wire) // upperCircuit
                9 -> volume = readVarint(buffer) // volume
                10 -> oi = readVarint(buffer).toDouble() // oi
                11 -> readDoubleOrFloat(buffer, wire) // oiDayHigh
                12 -> readDoubleOrFloat(buffer, wire) // oiDayLow
                13 -> ts = readVarint(buffer) // ts
                else -> skipField(buffer, wire)
            }
        }
        return MarketOhlcResult(open, high, low, close, volume, oi, ts)
    }

    private data class IndexOhlcResult(val open: Double, val high: Double, val low: Double, val close: Double, val ts: Long)

    private fun parseIndexOhlc(buffer: ByteBuffer): IndexOhlcResult {
        var open = 0.0
        var high = 0.0
        var low = 0.0
        var close = 0.0
        var ts = 0L

        while (buffer.hasRemaining()) {
            val tag = readVarint(buffer)
            val field = (tag ushr 3).toInt()
            val wire = (tag and 0x07).toInt()
            when (field) {
                1 -> open = readDoubleOrFloat(buffer, wire)
                2 -> high = readDoubleOrFloat(buffer, wire)
                3 -> low = readDoubleOrFloat(buffer, wire)
                4 -> close = readDoubleOrFloat(buffer, wire)
                5 -> ts = readVarint(buffer)
                else -> skipField(buffer, wire)
            }
        }
        return IndexOhlcResult(open, high, low, close, ts)
    }

    private fun readVarint(buffer: ByteBuffer): Long {
        var result = 0L
        var shift = 0
        while (buffer.hasRemaining()) {
            val b = buffer.get().toInt()
            result = result or ((b and 0x7F).toLong() shl shift)
            if ((b and 0x80) == 0) break
            shift += 7
            if (shift >= 64) break
        }
        return result
    }

    private fun readDoubleOrFloat(buffer: ByteBuffer, wireType: Int): Double {
        return when (wireType) {
            1 -> if (buffer.remaining() >= 8) buffer.double else 0.0
            5 -> if (buffer.remaining() >= 4) buffer.float.toDouble() else 0.0
            0 -> readVarint(buffer).toDouble()
            else -> {
                skipField(buffer, wireType)
                0.0
            }
        }
    }

    private fun sliceBuffer(buffer: ByteBuffer, length: Int): ByteBuffer {
        val safeLen = minOf(length.coerceAtLeast(0), buffer.remaining())
        val slice = buffer.slice().order(ByteOrder.LITTLE_ENDIAN)
        slice.limit(safeLen)
        buffer.position(buffer.position() + safeLen)
        return slice
    }

    private fun skipField(buffer: ByteBuffer, wireType: Int) {
        when (wireType) {
            0 -> readVarint(buffer)
            1 -> {
                val skip = minOf(8, buffer.remaining())
                buffer.position(buffer.position() + skip)
            }
            2 -> {
                val len = readVarint(buffer).toInt()
                val skip = minOf(len.coerceAtLeast(0), buffer.remaining())
                buffer.position(buffer.position() + skip)
            }
            5 -> {
                val skip = minOf(4, buffer.remaining())
                buffer.position(buffer.position() + skip)
            }
            else -> {
                // Unknown wire type, skip to end
                buffer.position(buffer.limit())
            }
        }
    }
}
