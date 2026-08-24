import os

filepath = "app/src/main/java/com/example/data/network/FyersMarketDataService.kt"
with open(filepath, "r") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if "suspend fun getOptionExpiries(symbol: String): Result<List<String>> = Result.failure(Exception(\"Not implemented\"))" in line:
        if any("getOptionExpiries" in l for l in new_lines):
            continue
    if "suspend fun getHistoricalCandles(symbol: String, interval: String, fromDate: String, toDate: String): Result<List<CandleData>> = Result.failure(Exception(\"Not implemented\"))" in line:
        continue
    new_lines.append(line)

with open(filepath, "w") as f:
    f.writelines(new_lines)
