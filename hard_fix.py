import re

def rewrite_main():
    path = "app/src/main/java/com/example/MainActivity.kt"
    with open(path, "r") as f:
        content = f.read()
    content = content.replace("onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) }\n                        )", "onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) }\n)")
    if "onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) }\n)" not in content:
        content = re.sub(r'onFyersLogin = \{ app, secret, codeOrToken -> viewModel\.connectFyers\(app, secret, codeOrToken\) \}\s*\n\s*\)', 'onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) }\n                            )', content)
    with open(path, "w") as f:
        f.write(content)

def rewrite_market_store():
    path = "app/src/main/java/com/example/data/model/MarketDataStore.kt"
    with open(path, "r") as f:
        content = f.read()
    # Check for empty const val and replace
    lines = content.split('\n')
    out = []
    for line in lines:
        if line.strip().startswith("const val") and "=" not in line:
            continue
        out.append(line)
    with open(path, "w") as f:
        f.write("\n".join(out))

def rewrite_engine():
    path = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
    with open(path, "r") as f:
        content = f.read()
    content = re.sub(r'val angelMarketDataService: AngelOneMarketDataService\? = null[\s\S]*?\{', 'val angelMarketDataService: AngelOneMarketDataService? = null\n) {', content)
    with open(path, "w") as f:
        f.write(content)

def rewrite_dialog():
    path = "app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt"
    with open(path, "r") as f:
        content = f.read()
    content = re.sub(r'onAngelLogin: \(\(String, String, String, String\) -> Unit\)\? = null[\s\S]*?\{', 'onAngelLogin: ((String, String, String, String) -> Unit)? = null\n) {', content)
    with open(path, "w") as f:
        f.write(content)

rewrite_main()
rewrite_market_store()
rewrite_engine()
rewrite_dialog()
print("Applied Hard Fix.")
