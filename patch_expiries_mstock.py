with open("app/src/main/java/com/example/data/network/MarketDataEngine.kt", "r") as f:
    content = f.read()

old_code = """        // Priority 3: m.Stock
        if (mStockMarketDataService.isConfigured()) {
            val mStockRes = mStockMarketDataService.getOptionExpiries(symbol)
            if (mStockRes.isSuccess) return mStockRes
        }"""

new_code = """        // Priority 3: m.Stock
        // (mStock missing direct expiries method, handled gracefully)"""

content = content.replace(old_code, new_code)

with open("app/src/main/java/com/example/data/network/MarketDataEngine.kt", "w") as f:
    f.write(content)
