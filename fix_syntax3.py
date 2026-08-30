import re

def fix_main_activity():
    path = "app/src/main/java/com/example/MainActivity.kt"
    with open(path, "r") as f:
        content = f.read()
    if 'onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) }\n)' in content:
        content = content.replace('onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) }\n)', 'onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) }\n                            )')
    with open(path, "w") as f:
        f.write(content)

def fix_market_data_store():
    path = "app/src/main/java/com/example/data/model/MarketDataStore.kt"
    with open(path, "r") as f:
        content = f.read()
    # Remove hanging empty const val entirely if there is one
    content = re.sub(r'const val.*?$', '', content, flags=re.MULTILINE)
    with open(path, "w") as f:
        f.write(content)

def fix_market_data_engine():
    path = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
    with open(path, "r") as f:
        content = f.read()
    content = content.replace('val angelMarketDataService: AngelOneMarketDataService? = null\n)', 'val angelMarketDataService: AngelOneMarketDataService? = null\n)')
    if 'val angelMarketDataService: AngelOneMarketDataService? = null\n)' not in content:
        content = re.sub(r'val angelMarketDataService: AngelOneMarketDataService\? = null\n\)', 'val angelMarketDataService: AngelOneMarketDataService? = null\n)', content)
    with open(path, "w") as f:
        f.write(content)

def fix_broker_connect_dialog():
    path = "app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt"
    with open(path, "r") as f:
        content = f.read()
    content = content.replace('onAngelLogin: ((String, String, String, String) -> Unit)? = null\n)', 'onAngelLogin: ((String, String, String, String) -> Unit)? = null\n)')
    with open(path, "w") as f:
        f.write(content)

fix_main_activity()
fix_market_data_store()
fix_market_data_engine()
fix_broker_connect_dialog()

print("Fixed syntax 3.")
