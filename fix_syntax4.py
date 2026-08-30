import re

def fix_main_activity():
    path = "app/src/main/java/com/example/MainActivity.kt"
    with open(path, "r") as f:
        content = f.read()
    content = content.replace('onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) }\n                            )', 'onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) }\n                        )')
    with open(path, "w") as f:
        f.write(content)

def fix_engine():
    path = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
    with open(path, "r") as f:
        content = f.read()
    content = content.replace('val angelMarketDataService: AngelOneMarketDataService? = null\n)', 'val angelMarketDataService: AngelOneMarketDataService? = null\n) {')
    if ') {' not in content:
        content = content.replace(') {', ')\n{')
    with open(path, "w") as f:
        f.write(content)

def fix_dialog():
    path = "app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt"
    with open(path, "r") as f:
        content = f.read()
    content = content.replace('onAngelLogin: ((String, String, String, String) -> Unit)? = null\n)', 'onAngelLogin: ((String, String, String, String) -> Unit)? = null\n) {')
    with open(path, "w") as f:
        f.write(content)

fix_main_activity()
fix_engine()
fix_dialog()
print("Fixed syntax 4.")
