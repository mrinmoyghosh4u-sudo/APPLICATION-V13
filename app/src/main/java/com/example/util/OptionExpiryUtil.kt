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

        // 2. Do not generate dynamic expiries locally based on user rule
        // Trading/API requests must only use real provider data.
        return emptyList()
    }

    /**
     * Convert display expiry string (e.g. "26 Aug 2026 (M)") to API format ("2026-08-26").
     */
    fun formatForApi(expiryDisplayStr: String): String {
        val date = parseExpiryDate(expiryDisplayStr) ?: return ""
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        sdf.timeZone = istTimeZone
        return sdf.format(date)
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
        val formats = listOf(
            "dd MMM yyyy",
            "dd-MMM-yyyy",
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyyMMdd"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                sdf.timeZone = istTimeZone
                val parsed = sdf.parse(clean)
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }
        return null
    }

    private fun formatDisplayExpiry(date: Date, isMonthly: Boolean): String {
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
            else -> Calendar.TUESDAY
        }
        return isLastDayOfWeekInMonth(cal, targetDay)
    }
}

