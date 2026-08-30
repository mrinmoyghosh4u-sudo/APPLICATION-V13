import re

def clean_trailing(path):
    with open(path, "r") as f:
        content = f.read()
    if content.count('{') > content.count('}'):
        content += "\n}\n"
    elif content.count('{') < content.count('}'):
        content = content[:content.rfind('}')]
    with open(path, "w") as f:
        f.write(content)


def fix_main_activity():
    path = "app/src/main/java/com/example/MainActivity.kt"
    with open(path, "r") as f:
        content = f.read()
    
    # MainActivity line 824 issue
    content = re.sub(r'onFyersLogin = \{ app, secret, codeOrToken -> viewModel\.connectFyers\(app, secret, codeOrToken\) \},\s*\n\s*\)', 'onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) }\n)', content)
    with open(path, "w") as f:
        f.write(content)

def fix_market_data_store():
    path = "app/src/main/java/com/example/data/model/MarketDataStore.kt"
    with open(path, "r") as f:
        content = f.read()
    content = re.sub(r'const val.*?=\s*\n', '', content)
    with open(path, "w") as f:
        f.write(content)

def fix_broker_manager():
    path = "app/src/main/java/com/example/data/network/BrokerManager.kt"
    with open(path, "r") as f:
        content = f.read()
    if content.count('{') > content.count('}'):
        content += "\n}\n"
    with open(path, "w") as f:
        f.write(content)

def fix_market_data_engine():
    path = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
    with open(path, "r") as f:
        content = f.read()
    content = re.sub(r'val angelMarketDataService: AngelOneMarketDataService\? = null,\n\)', 'val angelMarketDataService: AngelOneMarketDataService? = null\n)', content)
    with open(path, "w") as f:
        f.write(content)

def fix_broker_connect_dialog():
    path = "app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt"
    with open(path, "r") as f:
        content = f.read()
    content = re.sub(r'onAngelLogin: \(\(String, String, String, String\) -> Unit\)\? = null,\n\)', 'onAngelLogin: ((String, String, String, String) -> Unit)? = null\n)', content)
    with open(path, "w") as f:
        f.write(content)


fix_main_activity()
fix_market_data_store()
fix_broker_manager()
fix_market_data_engine()
fix_broker_connect_dialog()
clean_trailing("app/src/main/java/com/example/data/network/ProviderHealthManager.kt")
clean_trailing("app/src/main/java/com/example/ui/components/CommonComponents.kt")
print("Syntax fixed 2.")
