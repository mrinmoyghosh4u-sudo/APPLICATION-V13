import re
content = open("app/src/main/java/com/example/data/network/InstrumentMasterService.kt").read()

# I will find the duplicate block and remove it
bad_part = """    }) return uppercaseSymbol

        // Search by symbol or name
        val match = instrumentMap.values.find {
            (it.symbol.equals(uppercaseSymbol, ignoreCase = true) || it.name.equals(uppercaseSymbol, ignoreCase = true)) &&
            (exchange.isBlank() || it.exch_seg.equals(exchange, ignoreCase = true))
        }
        return match?.token
    }"""
content = content.replace(bad_part, "")

open("app/src/main/java/com/example/data/network/InstrumentMasterService.kt", "w").write(content)
