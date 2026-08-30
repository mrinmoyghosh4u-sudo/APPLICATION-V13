import re

# Fix UpstoxMarketDataService
with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "r") as f:
    upstox = f.read()

# Fix 1: onClosed should not reconnect if code == 1000 (normal) or code == 1008 (auth fail)
upstox = upstox.replace("""                            com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.UPSTOX, "OFFLINE")
                            scheduleReconnect()
                        }""", """                            com.example.data.model.MarketDataStore.setSourceHealth(com.example.data.model.MarketDataSourceNames.UPSTOX, "OFFLINE")
                            if (code != 1000 && code != 1008 && code != 1001) {
                                scheduleReconnect()
                            }
                        }""")

# Fix 2: Auth response handling
upstox = upstox.replace("""                    Log.e(TAG, "[UPSTOX_ERROR] Failed to obtain V3 WebSocket auth: HTTP $code")
                    scheduleReconnect()
                    return@launch""", """                    Log.e(TAG, "[UPSTOX_ERROR] Failed to obtain V3 WebSocket auth: HTTP $code")
                    if (code == 401 || code == 403) {
                        sessionManager.clearUpstoxSession()
                    } else {
                        scheduleReconnect()
                    }
                    return@launch""")

# Fix 3: Stale checker should pause when market is closed
upstox = upstox.replace("""                if (isConnected && hasFirstTick && (_connectionState.value == "LIVE" || _connectionState.value == "SUBSCRIBED")) {
                    val age = getTickAgeMs()""", """                if (isConnected && hasFirstTick && (_connectionState.value == "LIVE" || _connectionState.value == "SUBSCRIBED")) {
                    val marketDetail = com.example.util.MarketStatusUtil.getDetailedMarketStatus("NSE")
                    if (!marketDetail.isOpen) {
                        continue
                    }
                    val age = getTickAgeMs()""")

with open("app/src/main/java/com/example/data/network/UpstoxMarketDataService.kt", "w") as f:
    f.write(upstox)


# Fix FyersMarketDataService
with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r") as f:
    fyers = f.read()

# Fix 1: onClosed should not reconnect on 1000 or 1008
fyers = fyers.replace("""                Log.i(TAG, "[FYERS_DISCONNECTED] Socket closed ($code: $reason)")
                scheduleReconnect()
            }""", """                Log.i(TAG, "[FYERS_DISCONNECTED] Socket closed ($code: $reason)")
                if (code != 1000 && code != 1008 && code != 1001) {
                    scheduleReconnect()
                }
            }""")

with open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "w") as f:
    f.write(fyers)

# Fix AngelOneMarketDataService
with open("app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt", "r") as f:
    angel = f.read()

angel = angel.replace("""                Log.d("SmartStream", "WebSocket Closed: $reason")
                scheduleReconnect()
            }""", """                Log.d("SmartStream", "WebSocket Closed: $reason")
                if (code != 1000 && code != 1008 && code != 1001) {
                    scheduleReconnect()
                }
            }""")

with open("app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt", "w") as f:
    f.write(angel)

print("Re-connect loops patched.")
