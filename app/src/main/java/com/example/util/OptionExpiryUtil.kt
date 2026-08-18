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

        // 2. Generate dynamic upcoming expiries based on current exchange rules
        return generateDynamicExpiriesIST(symbol)
            .filter { it.rawDate.time >= activeCutoff.time }
            .sortedBy { it.rawDate.time }
            .map { it.dateString }
            .distinct()
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

    private fun generateDynamicExpiriesIST(symbol: String): List<OptionExpiryInfo> {
        val cal = Calendar.getInstance(istTimeZone)
        val activeCutoff = getActiveCutoffTimeIST()
        val expiries = mutableListOf<OptionExpiryInfo>()
        val symUpper = symbol.uppercase()

        val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        displayFormat.timeZone = istTimeZone

        when {
            // NIFTY: Weekly expiry every Tuesday. Last Tuesday of month is monthly.
            symUpper == "NIFTY" || symUpper.contains("NIFTY 50") -> {
                val tempCal = cal.clone() as Calendar
                var count = 0
                while (count < 6) {
                    if (tempCal.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY && tempCal.time >= activeCutoff) {
                        val isMonthly = isLastDayOfWeekInMonth(tempCal, Calendar.TUESDAY)
                        val formatted = "${displayFormat.format(tempCal.time)} ${if (isMonthly) "(M)" else "(W)"}"
                        expiries.add(OptionExpiryInfo(formatted, tempCal.time, isMonthly = isMonthly, isWeekly = !isMonthly))
                        count++
                    }
                    tempCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // MONTHLY LAST TUESDAY EXPIRIES
            symUpper.contains("BANKNIFTY") || symUpper.contains("FINNIFTY") || 
            symUpper.contains("MIDCPNIFTY") || symUpper.contains("NIFTYNEXT50") || symUpper.contains("NIFTY NEXT 50") -> {
                val tempCal = cal.clone() as Calendar
                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                var count = 0
                while (count < 6) {
                    val lastTuesday = getLastDayOfWeekOfMonth(tempCal, Calendar.TUESDAY)
                    if (lastTuesday.time >= activeCutoff) {
                        val formatted = "${displayFormat.format(lastTuesday.time)} (M)"
                        expiries.add(OptionExpiryInfo(formatted, lastTuesday.time, isMonthly = true, isWeekly = false))
                        count++
                    }
                    tempCal.add(Calendar.MONTH, 1)
                }
            }

            // SENSEX: Weekly expiry every Thursday. Last Thursday of month is monthly.
            symUpper.contains("SENSEX") -> {
                val tempCal = cal.clone() as Calendar
                var count = 0
                while (count < 6) {
                    if (tempCal.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY && tempCal.time >= activeCutoff) {
                        val isMonthly = isLastDayOfWeekInMonth(tempCal, Calendar.THURSDAY)
                        val formatted = "${displayFormat.format(tempCal.time)} ${if (isMonthly) "(M)" else "(W)"}"
                        expiries.add(OptionExpiryInfo(formatted, tempCal.time, isMonthly = isMonthly, isWeekly = !isMonthly))
                        count++
                    }
                    tempCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // BANKEX: Monthly expiry -> Last Thursday of the month.
            symUpper.contains("BANKEX") -> {
                val tempCal = cal.clone() as Calendar
                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                var count = 0
                while (count < 6) {
                    val lastThursday = getLastDayOfWeekOfMonth(tempCal, Calendar.THURSDAY)
                    if (lastThursday.time >= activeCutoff) {
                        val formatted = "${displayFormat.format(lastThursday.time)} (M)"
                        expiries.add(OptionExpiryInfo(formatted, lastThursday.time, isMonthly = true, isWeekly = false))
                        count++
                    }
                    tempCal.add(Calendar.MONTH, 1)
                }
            }

            // MCX COMMODITIES (CRUDE OIL, NATURAL GAS, GOLD, SILVER)
            symUpper.contains("CRUDE") -> {
                expiries.addAll(generateCommodityExpiries(cal, activeCutoff, targetDayOfMonth = 17, displayFormat))
            }
            symUpper.contains("NATURAL") -> {
                expiries.addAll(generateCommodityExpiries(cal, activeCutoff, targetDayOfMonth = 24, displayFormat))
            }
            symUpper.contains("GOLD") || symUpper.contains("SILVER") -> {
                expiries.addAll(generateCommodityExpiries(cal, activeCutoff, targetDayOfMonth = 26, displayFormat))
            }

            // Default fallback (Weekly Thursdays)
            else -> {
                val tempCal = cal.clone() as Calendar
                var count = 0
                while (count < 6) {
                    if (tempCal.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY && tempCal.time >= activeCutoff) {
                        val isMonthly = isLastDayOfWeekInMonth(tempCal, Calendar.THURSDAY)
                        val formatted = "${displayFormat.format(tempCal.time)} ${if (isMonthly) "(M)" else "(W)"}"
                        expiries.add(OptionExpiryInfo(formatted, tempCal.time, isMonthly = isMonthly, isWeekly = !isMonthly))
                        count++
                    }
                    tempCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }

        return expiries
    }

    private fun generateCommodityExpiries(
        startCal: Calendar,
        activeCutoff: Date,
        targetDayOfMonth: Int,
        format: SimpleDateFormat
    ): List<OptionExpiryInfo> {
        val list = mutableListOf<OptionExpiryInfo>()
        val tempCal = startCal.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)
        var count = 0
        while (count < 6) {
            val maxDay = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val dayToSet = targetDayOfMonth.coerceAtMost(maxDay)
            val expCal = tempCal.clone() as Calendar
            expCal.set(Calendar.DAY_OF_MONTH, dayToSet)
            // Adjust for weekend
            if (expCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) expCal.add(Calendar.DAY_OF_MONTH, -1)
            if (expCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) expCal.add(Calendar.DAY_OF_MONTH, -2)

            if (expCal.time >= activeCutoff) {
                val formatted = "${format.format(expCal.time)} (M)"
                list.add(OptionExpiryInfo(formatted, expCal.time, isMonthly = true, isWeekly = false))
                count++
            }
            tempCal.add(Calendar.MONTH, 1)
        }
        return list
    }

    private fun getLastDayOfWeekOfMonth(monthCal: Calendar, targetDayOfWeek: Int): Calendar {
        val cal = monthCal.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        while (cal.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return cal
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

