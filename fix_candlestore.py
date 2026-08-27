import re

with open("app/src/main/java/com/example/util/indicators/CandleStore.kt", "r") as f:
    content = f.read()

# Replace normalizeKey
new_norm = """
    fun normalizeKey(symbolOrKey: String): String {
        val raw = symbolOrKey.trim().uppercase()
        try {
            val master = com.example.data.network.InstrumentMasterService.instance
            val token = master?.resolveAngelToken(raw) ?: master?.resolveAngelToken(raw.removePrefix("NSE:").removePrefix("BSE:").removePrefix("MCX:"))
            if (!token.isNullOrBlank()) {
                return token
            }
        } catch (e: Exception) {}
        
        if (raw.contains("|")) {
            return raw
        }
        val upper = raw
        if (!upper.contains(" CE") && !upper.contains(" PE") && !upper.contains(" FUT") && !upper.contains(" ")) {
            return when (upper) {
                "NIFTY", "NIFTY 50", "NIFTY50" -> "NIFTY 50"
                "BANKNIFTY", "NIFTY BANK", "BANK NIFTY" -> "BANKNIFTY"
                "FINNIFTY", "NIFTY FIN SERVICE", "FIN NIFTY" -> "FINNIFTY"
                "MIDCPNIFTY", "MIDCAP NIFTY" -> "MIDCPNIFTY"
                "SENSEX", "BSESN", "BSE SENSEX" -> "SENSEX"
                "BANKEX", "BSE BANKEX" -> "BANKEX"
                else -> upper
            }
        }
        return upper
    }
"""

content = re.sub(r'fun normalizeKey\(symbolOrKey: String\): String \{.*?(?=fun normalizeTimeframe)', new_norm, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/util/indicators/CandleStore.kt", "w") as f:
    f.write(content)
