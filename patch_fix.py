import re

def fix_instrument_resolvers():
    path = "app/src/main/java/com/example/data/network/InstrumentResolvers.kt"
    with open(path, "r") as f:
        content = f.read()
    
    # We stripped the mstock class but might have left unbalanced braces
    if content.count('{') > content.count('}'):
        content += "\n}\n"
        with open(path, "w") as f:
            f.write(content)
            
def fix_market_data_engine():
    path = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
    with open(path, "r") as f:
        content = f.read()
    content = content.replace("private val : MStockMarketDataService,", "")
    content = re.sub(r'private val\s*:.*?,', '', content)
    with open(path, "w") as f:
        f.write(content)

def fix_provider_health_manager():
    path = "app/src/main/java/com/example/data/network/ProviderHealthManager.kt"
    with open(path, "r") as f:
        content = f.read()
    
    if "PROVIDER_MSTOCK" in content:
         content = re.sub(r'const val PROVIDER_MSTOCK.*?$', '', content, flags=re.MULTILINE)
    
    if content.count('{') > content.count('}'):
        content += "\n}\n"
    with open(path, "w") as f:
        f.write(content)

def fix_session_manager():
    path = "app/src/main/java/com/example/data/network/SessionManager.kt"
    with open(path, "r") as f:
        content = f.read()
    if content.count('{') > content.count('}'):
        content += "\n}\n"
    with open(path, "w") as f:
        f.write(content)

def fix_broker_connect_dialog():
    path = "app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt"
    with open(path, "r") as f:
        content = f.read()
    content = re.sub(r'onMStockLogin:.*?,', '', content)
    with open(path, "w") as f:
        f.write(content)

def fix_common_components():
    path = "app/src/main/java/com/example/ui/components/CommonComponents.kt"
    with open(path, "r") as f:
        content = f.read()
    if content.count('{') > content.count('}'):
        content += "\n}\n"
    with open(path, "w") as f:
        f.write(content)

def fix_diagnostics_screen():
    path = "app/src/main/java/com/example/ui/screens/DiagnosticsScreen.kt"
    with open(path, "r") as f:
        content = f.read()
    if content.count('{') > content.count('}'):
        content += "\n}\n"
    content = re.sub(r'val mStockConnectionState by.*?\n', '', content)
    content = re.sub(r'mStockState: String,', '', content)
    with open(path, "w") as f:
        f.write(content)
        
fix_instrument_resolvers()
fix_market_data_engine()
fix_provider_health_manager()
fix_session_manager()
fix_broker_connect_dialog()
fix_common_components()
fix_diagnostics_screen()
print("Fixed Kotlin syntax errors.")
