sed -i '232,234c\
                val spotPrice = 24850.40\
                val step = if (symbol.contains("BANKNIFTY") || symbol.contains("SENSEX") || symbol.contains("BANKEX")) 100.0 else 50.0\
                val atmStrike = (spotPrice / step).let { Math.round(it) * step }' app/src/main/java/com/example/data/network/DhanBrokerService.kt
