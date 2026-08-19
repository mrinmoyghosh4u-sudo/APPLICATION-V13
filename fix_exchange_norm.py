import os

files_to_fix = [
    "app/src/main/java/com/example/data/network/InstrumentMasterService.kt",
    "app/src/main/java/com/example/data/network/AngelOneMarketDataService.kt",
    "app/src/main/java/com/example/data/network/AngelOneBrokerService.kt"
]

norm_func = """
    private fun getExchangeType(exchSeg: String): Int {
        return when (exchSeg.uppercase()) {
            "NSE", "NSE_CM" -> 1
            "NFO", "NSE_FO" -> 2
            "BSE", "BSE_CM" -> 3
            "BFO", "BSE_FO" -> 4
            "MCX", "MCX_FO" -> 5
            "NCDEX" -> 7
            "CDS" -> 9
            else -> 1
        }
    }
"""

for f in files_to_fix:
    if not os.path.exists(f): continue
    content = open(f).read()
    
    # replace getExchangeType body
    import re
    content = re.sub(
        r'private fun getExchangeType\([^)]+\):\s*Int\s*\{.*?\n\s*\}', 
        norm_func.strip(), 
        content, 
        flags=re.DOTALL
    )
    
    open(f, "w").write(content)

