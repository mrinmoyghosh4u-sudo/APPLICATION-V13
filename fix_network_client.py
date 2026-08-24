import os

filepath = "app/src/main/java/com/example/data/network/BrokerNetworkClient.kt"
with open(filepath, "r") as f:
    content = f.read()

import re

# Remove trailing brace and append it at the end
content = content.replace("    }\n}\n\n\n    // =========================================\n    // FYERS", "    }\n\n    // =========================================\n    // FYERS")

if "FyersApi::class.java)\n    }" not in content:
    pass
else:
    content = content.replace("FyersApi::class.java)\n    }", "FyersApi::class.java)\n    }\n}")

with open(filepath, "w") as f:
    f.write(content)
