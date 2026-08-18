import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun main() {
    val expiry = "28 Feb 2024 (M)"
    val clean = expiry.replace("(W)", "").replace("(M)", "").trim()
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
            val parsed = sdf.parse(clean)
            if (parsed != null) {
                val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                println(apiFormat.format(parsed))
                return
            }
        } catch (_: Exception) {}
    }
}
