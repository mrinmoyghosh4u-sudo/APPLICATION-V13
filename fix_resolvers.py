import re

filepath = "app/src/main/java/com/example/data/network/InstrumentResolvers.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace(
"""        return CanonicalInstrument(
            exchange = normExch,
            segment = segment,
            symbol = cleanSym,
            displayName = inst?.name ?: cleanSym,
            instrumentType = instType,
            instrumentKey = "",
            id = id,
            lotSize = if (lotSize > 0) lotSize else 1
        )""",
"""        return CanonicalInstrument(
            exchange = normExch,
            segment = segment,
            symbol = cleanSym,
            displayName = cleanSym,
            instrumentType = instType,
            instrumentKey = "",
            token = mappedKey,
            lotSize = if (lotSize > 0) lotSize else 1
        )"""
)

with open(filepath, "w") as f:
    f.write(content)
