package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class DetailedMarketStatus(
    val isOpen: Boolean,
    val statusText: String, // "MARKET OPEN" or "MARKET CLOSED"
    val currentTimeIST: String, // "03:30:00 PM IST"
    val nextOpeningTimeText: String, // "Opens Tomorrow at 09:15 AM IST"
    val fullDetailLabel: String
)

object MarketStatusUtil {

    fun getMarketStatus(exchange: String = "NSE"): Pair<String, String> {
        val detail = getDetailedMarketStatus(exchange)
        return detail.statusText to detail.currentTimeIST
    }

    fun getDetailedMarketStatus(exchange: String = "NSE"): DetailedMarketStatus {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        val timeInMinutes = hour * 60 + minute

        val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        val timeStr = "${sdf.format(cal.time)} IST"

        val isMcx = exchange.equals("MCX", ignoreCase = true)
        val openMinutes = if (isMcx) 9 * 60 else 9 * 60 + 15 // 09:00 AM for MCX, 09:15 AM for NSE/BSE
        val closeMinutes = if (isMcx) 23 * 60 + 30 else 15 * 60 + 30 // 11:30 PM for MCX, 03:30 PM for NSE/BSE

        val isWeekday = dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
        val isOpen = isWeekday && (timeInMinutes in openMinutes..closeMinutes)

        val openTimeFormatted = if (isMcx) "09:00 AM IST" else "09:15 AM IST"

        val nextOpeningText = when {
            isOpen -> "Market is currently trading live."
            dayOfWeek == Calendar.FRIDAY && timeInMinutes > closeMinutes -> "Opens Mon at $openTimeFormatted"
            dayOfWeek == Calendar.SATURDAY -> "Opens Mon at $openTimeFormatted"
            dayOfWeek == Calendar.SUNDAY -> "Opens Mon at $openTimeFormatted"
            timeInMinutes < openMinutes -> "Opens Today at $openTimeFormatted"
            else -> "Opens Tomorrow at $openTimeFormatted"
        }

        val statusText = if (isOpen) "MARKET OPEN" else "MARKET CLOSED"
        val detailLabel = if (isOpen) {
            if (isMcx) "MCX Commodities Session (09:00 - 23:30 IST)" else "$exchange Session (09:15 - 15:30 IST)"
        } else {
            "$exchange Closed • $nextOpeningText"
        }

        return DetailedMarketStatus(
            isOpen = isOpen,
            statusText = statusText,
            currentTimeIST = timeStr,
            nextOpeningTimeText = nextOpeningText,
            fullDetailLabel = detailLabel
        )
    }
}
