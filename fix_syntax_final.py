import re

def fix_main():
    path = "app/src/main/java/com/example/MainActivity.kt"
    with open(path, "r") as f:
        content = f.read()
    
    # Let's just fix the trailing parenthesis directly
    content = content.replace("onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) }\n                            )", "onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) }\n                        )")
    if "viewModel.connectFyers(app, secret, codeOrToken) }" in content and ")" not in content.split("viewModel.connectFyers(app, secret, codeOrToken) }")[1]:
        content = content.replace("viewModel.connectFyers(app, secret, codeOrToken) }", "viewModel.connectFyers(app, secret, codeOrToken) }\n)")

    with open(path, "w") as f:
        f.write(content)
        
def fix_store():
    path = "app/src/main/java/com/example/data/model/MarketDataStore.kt"
    with open(path, "r") as f:
        content = f.read()
    content = content.replace("const val ", "//const val ") # Comment it out just in case
    with open(path, "w") as f:
        f.write(content)

fix_main()
fix_store()
print("Fixed.")
