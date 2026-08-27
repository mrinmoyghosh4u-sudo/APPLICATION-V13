package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class OptionExpiryInfo(
    val dateString: String,
    val rawDate: Date,
    val isMonthly: Boolean,
    val isWeekly: Boolean
)

/**
 * Common Canonical Expiry Resolver for all Brokers & Exchanges (NSE, BSE, MCX).
 * Normalizes between Upstox (yyyy-MM-dd), Angel One (ddMMMyyyy), Fyers, and UI display formats.
 */
object OptionExpiryUtil {

    private val istTimeZone = TimeZone.getTimeZone("Asia/Kolkata")

    /**
     * Dynamically gets upcoming valid option expiries for a given index/symbol in IST.
     * Always filters out past/expired dates and sorts by nearest date ascending.
     */
    fun getUpcomingExpiriesForSymbol(symbol: String, liveBrokerExpiries: List<String> = emptyList()): List<String> {
        val activeCutoff = getActiveCutoffTimeIST()

        // 1. If live broker expiries are provided, parse & filter out expired dates
        if (liveBrokerExpiries.isNotEmpty()) {
            val parsed = liveBrokerExpiries.mapNotNull { expStr ->
                parseExpiryDate(expStr)?.let { date ->
                    val displayStr = formatDisplayExpiry(date, isMonthlyExpiryDate(symbol, date))
                    displayStr to date
                }
            }.filter { (_, date) ->
                date.time >= activeCutoff.time
            }.sortedBy { (_, date) ->
                date.time
            }.map { it.first }

            if (parsed.isNotEmpty()) {
                return parsed.distinct()
            }
        }

        // 2. Query InstrumentMasterService for real exchange/broker option contracts
        val masterExpiries = com.example.data.network.InstrumentMasterService.instance?.getOptionExpiries(symbol) ?: emptyList()
        if (masterExpiries.isNotEmpty()) {
            val parsed = masterExpiries.mapNotNull { expStr ->
                parseExpiryDate(expStr)?.let { date ->
                    val displayStr = formatDisplayExpiry(date, isMonthlyExpiryDate(symbol, date))
                    displayStr to date
                }
            }.filter { (_, date) ->
                date.time >= activeCutoff.time
            }.sortedBy { (_, date) ->
                date.time
            }.map { it.first }

            if (parsed.isNotEmpty()) {
                return parsed.distinct()
            }
        }

        // 3. Dynamic Fallback Generation in case liveBrokerExpiries and Instrument Master are empty
        val symUpper = symbol.uppercase()
        val targetDayOfWeek = when {
            symUpper.contains("BANKNIFTY") -> Calendar.WEDNESDAY
            symUpper.contains("SENSEX") || symUpper.contains("BANKEX") -> Calendar.FRIDAY
            symUpper.contains("FINNIFTY") -> Calendar.TUESDAY
            symUpper.contains("MIDCPNIFTY") -> Calendar.MONDAY
            else -> Calendar.THURSDAY // NIFTY and default on Thursday
        }

        val fallbacks = mutableListOf<String>()
        val cal = Calendar.getInstance(istTimeZone)
        cal.time = activeCutoff

        // Advance to the next target day of week
        while (cal.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Generate next 4 expiries
        for (i in 1..4) {
            val date = cal.time
            val isMonthly = isMonthlyExpiryDate(symbol, date)
            fallbacks.add(formatDisplayExpiry(date, isMonthly))
            cal.add(Calendar.DAY_OF_MONTH, 7)
        }

        return fallbacks
    }

    /**
     * Convert display expiry string (e.g. "26 Aug 2026 (M)") or any format to API format ("2026-08-26").
     */
    fun formatForApi(expiryDisplayStr: String): String {
        if (expiryDisplayStr.isBlank()) return ""
        val date = parseExpiryDate(expiryDisplayStr) ?: return expiryDisplayStr.trim()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        sdf.timeZone = istTimeZone
        return sdf.format(date)
    }

    /**
     * Convert to Angel One format (e.g. "26AUG2026").
     */
    fun formatForAngel(expiryDisplayStr: String): String {
        if (expiryDisplayStr.isBlank()) return ""
        val date = parseExpiryDate(expiryDisplayStr) ?: return expiryDisplayStr.trim()
        val sdf = SimpleDateFormat("ddMMMyyyy", Locale.ENGLISH)
        sdf.timeZone = istTimeZone
        return sdf.format(date).uppercase()
    }

    /**
     * Normalize any expiry format to standard display format ("26 Aug 2026 (M/W)").
     */
    fun formatToDisplay(expiryStr: String, symbol: String = ""): String {
        val date = parseExpiryDate(expiryStr) ?: return expiryStr
        return formatDisplayExpiry(date, isMonthlyExpiryDate(symbol, date))
    }

    private fun getActiveCutoffTimeIST(): Date {
        val cal = Calendar.getInstance(istTimeZone)
        // If today is an expiry date and market is closed (past 15:30 IST), cutoff is tomorrow
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        if (hour > 15 || (hour == 15 && minute >= 30)) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    fun parseExpiryDate(expiryStr: String): Date? {
        val clean = expiryStr.replace(Regex("\\s*\\([WM]\\)"), "").trim()
        if (clean.isBlank()) return null
        val formats = listOf(
            "dd MMM yyyy",
            "dd-MMM-yyyy",
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "ddMMMyyyy",
            "ddMMMyy",
            "yyyyMMdd",
            "yyyy/MM/dd",
            "dd-MMM-yy",
            "dd MMM yy"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                sdf.timeZone = istTimeZone
                sdf.isLenient = false
                val parsed = sdf.parse(clean)
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }
        return null
    }

    fun formatDisplayExpiry(date: Date, isMonthly: Boolean): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        sdf.timeZone = istTimeZone
        val tag = if (isMonthly) "(M)" else "(W)"
        return "${sdf.format(date)} $tag"
    }

    private fun isLastDayOfWeekInMonth(cal: Calendar, targetDayOfWeek: Int): Boolean {
        val testCal = cal.clone() as Calendar
        testCal.add(Calendar.DAY_OF_MONTH, 7)
        return testCal.get(Calendar.MONTH) != cal.get(Calendar.MONTH)
    }

    private fun isMonthlyExpiryDate(symbol: String, date: Date): Boolean {
        val cal = Calendar.getInstance(istTimeZone)
        cal.time = date
        val symUpper = symbol.uppercase()
        val targetDay = when {
            symUpper.contains("SENSEX") || symUpper.contains("BANKEX") -> Calendar.THURSDAY
            symUpper.contains("CRUDE") || symUpper.contains("MCX") -> Calendar.TUESDAY
            else -> Calendar.TUESDAY
        }
        return isLastDayOfWeekInMonth(cal, targetDay)
    }
}

