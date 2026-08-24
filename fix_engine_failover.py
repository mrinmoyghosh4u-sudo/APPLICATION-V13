import os

filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

# We need to remove the injected code from evaluateLiveLtpFailover
# The injected code is:
#             // Priority 1: Fyers
#         if (fyersMarketDataService?.isConfigured() == true) {
# ... to
#             healthManager.logFailover("FYERS", ProviderHealthManager.PROVIDER_ANGEL_ONE)
#         }
#         
#         // Priority 2: Angel One
# Let's find it.

pattern_to_remove = r"            // Priority 1: Fyers\s+if \(fyersMarketDataService\?\.isConfigured\(\) == true\) \{.*?\n        \}\n        \n        // Priority 2: Angel One"
# replace with:
#             // Priority 1: Angel One

content = re.sub(pattern_to_remove, "            // Priority 1: Angel One", content, flags=re.DOTALL)

with open(filepath, "w") as f:
    f.write(content)

