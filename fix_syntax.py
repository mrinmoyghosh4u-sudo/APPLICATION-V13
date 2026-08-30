import re

def clean_file(path):
    with open(path, "r") as f:
        lines = f.readlines()
        
    # Remove excessive trailing braces
    text = "".join(lines)
    while text.endswith("}\n}\n"):
        text = text[:-2]
        
    with open(path, "w") as f:
        f.write(text)

def fix_instrument_resolvers():
    path = "app/src/main/java/com/example/data/network/InstrumentResolvers.kt"
    with open(path, "r") as f:
        content = f.read()
    # Find CanonicalInstrument creation return block and ensure it ends properly
    if "return CanonicalInstrument(" in content:
        content = re.sub(r'return CanonicalInstrument\([\s\S]*?lotSize = if \(lotSize > 0\) lotSize else 1\n        \)\n    \}\n\}[\s\S]*', 'return CanonicalInstrument(\n            exchange = normExch,\n            segment = segment,\n            symbol = cleanSym,\n            displayName = inst?.name ?: cleanSym,\n            instrumentType = instType,\n            instrumentKey = "",\n            token = token,\n            lotSize = if (lotSize > 0) lotSize else 1\n        )\n    }\n}\n', content)
    with open(path, "w") as f:
        f.write(content)

def fix_market_data_engine():
    path = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
    with open(path, "r") as f:
        content = f.read()
    content = re.sub(r',\s*\n\s*\)', '\n)', content)
    with open(path, "w") as f:
        f.write(content)
        
def fix_broker_connect_dialog():
    path = "app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt"
    with open(path, "r") as f:
        lines = f.readlines()
    with open(path, "w") as f:
        for line in lines:
            if "onMStockLogin" in line:
                continue
            if "val brokerHealth = providerHealth[ProviderHealthManager.PROVIDER_MSTOCK]" in line:
                continue
            f.write(line)

fix_instrument_resolvers()
fix_market_data_engine()
fix_broker_connect_dialog()
clean_file("app/src/main/java/com/example/data/network/ProviderHealthManager.kt")
clean_file("app/src/main/java/com/example/data/network/SessionManager.kt")
clean_file("app/src/main/java/com/example/ui/components/CommonComponents.kt")
clean_file("app/src/main/java/com/example/ui/screens/DiagnosticsScreen.kt")
print("Syntax fixed.")
