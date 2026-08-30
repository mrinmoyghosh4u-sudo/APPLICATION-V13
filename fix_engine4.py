import re

filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

# I see `return angelRes.map { kotlin.collections.emptyList<OptionStrikeItem>() }(symbol, expiry ?: "", 0.0, it) }` due to my bad regex replacement.
content = content.replace('return angelRes.map { kotlin.collections.emptyList<OptionStrikeItem>() }(symbol, expiry ?: "", 0.0, it) }', 'return Result.failure(Exception("Not implemented"))')
content = content.replace('return fyersRes.map { kotlin.collections.emptyList<OptionStrikeItem>() }(symbol, expiry ?: "", 0.0, it) }', 'return Result.failure(Exception("Not implemented"))')

with open(filepath, "w") as f:
    f.write(content)

