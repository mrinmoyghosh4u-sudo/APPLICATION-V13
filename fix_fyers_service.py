import os

filepath = "app/src/main/java/com/example/data/network/FyersMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

# Fix imports
content = content.replace("import com.example.data.model.CandleData\n", "")
content = content.replace("import com.example.ui.components.CandleData as UICandleData\n", "import com.example.ui.components.CandleData\n")
content = content.replace("override suspend fun getHistoricalCandles(symbol: String, interval: String): Result<List<CandleData>> =", "override suspend fun getHistoricalCandles(symbol: String, interval: String, fromDate: String, toDate: String): Result<List<CandleData>> =")
content = content.replace("val toDate = java.util.Date()", "val toDateObj = java.util.Date()")
content = content.replace("val fromDate = java.util.Date(toDate.time - (10 * 24 * 60 * 60 * 1000L))", "val fromDateObj = java.util.Date(toDateObj.time - (10 * 24 * 60 * 60 * 1000L))")
content = content.replace("from = sdf.format(fromDate)", "from = fromDate.ifBlank { sdf.format(fromDateObj) }")
content = content.replace("to = sdf.format(toDate)", "to = toDate.ifBlank { sdf.format(toDateObj) }")

with open(filepath, "w") as f:
    f.write(content)
