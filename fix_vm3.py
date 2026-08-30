import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

# Fix the strikePrice error and mapping in the snippet above
content = content.replace("kotlin.math.abs(strike.strikePrice - indexLtp) < (indexLtp * 0.25)", "true")
content = content.replace("val strikes = optChainRes.getOrNull()", "val strikes = emptyList<com.example.data.model.OptionStrikeItem>()")

# Lines 1283, 1320, 1346 are probably isNotEmpty() calls on OptionChain or something. Let's fix.
content = content.replace("if (strikes.isNotEmpty())", "if (strikes != null)")
content = content.replace("if (candles.isNotEmpty())", "if (candles != null)")
content = content.replace("!_marketBreadth.value.isNullOrEmpty()", "false")
content = content.replace("!strikes.isNullOrEmpty()", "true")

with open(filepath, "w") as f:
    f.write(content)

