import os

filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

# In evaluateLiveLtpFailover:
# It might look like:
#         } else {
#             // Priority 1: Fyers
#         if (fyersMarketDataService?.isConfigured() == true) {

content = re.sub(r'        \} else \{\n            // Priority 1: Fyers\s+if \(fyersMarketDataService\?\.isConfigured\(\) == true\) \{[\s\S]*?healthManager\.logFailover\("FYERS", ProviderHealthManager\.PROVIDER_ANGEL_ONE\)\n        \}\n        \n        // Priority 2: Angel One', r'        } else {\n            // Priority 1: Angel One', content)


# In getOptionChain:
content = re.sub(r'    suspend fun getOptionChain\(symbol: String, expiry: String = ""\): Result<List<OptionStrikeItem>> \{\n        // Priority 1: Fyers\s+if \(fyersMarketDataService\?\.isConfigured\(\) == true\) \{[\s\S]*?healthManager\.logFailover\("FYERS", ProviderHealthManager\.PROVIDER_ANGEL_ONE\)\n        \}\n        \n        // Priority 2: Angel One', r'    suspend fun getOptionChain(symbol: String, expiry: String = ""): Result<List<OptionStrikeItem>> {\n        // Priority 1: Angel One', content)

with open(filepath, "w") as f:
    f.write(content)
