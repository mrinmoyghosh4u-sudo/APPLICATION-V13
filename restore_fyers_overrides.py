import os

filepath = "app/src/main/java/com/example/data/network/FyersMarketDataService.kt"
with open(filepath, "r") as f:
    content = f.read()

import re
content = content.replace("    fun onOpen(", "    override fun onOpen(")
content = content.replace("    fun onMessage(", "    override fun onMessage(")
content = content.replace("    fun onClosed(", "    override fun onClosed(")
content = content.replace("    fun onFailure(", "    override fun onFailure(")

# What about conflicting overloads? 
# "Conflicting overloads: suspend fun getOptionExpiries"
# "Conflicting overloads: suspend fun getHistoricalCandles"
# I might have duplicated the functions when I replaced them!
