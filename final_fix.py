import re

def fix_main():
    path = "app/src/main/java/com/example/MainActivity.kt"
    with open(path, "r") as f:
        content = f.read()
    content = content.replace("onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) }\n)", "onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) }\n                            )")
    with open(path, "w") as f:
        f.write(content)
        
def fix_store():
    path = "app/src/main/java/com/example/data/model/MarketDataStore.kt"
    with open(path, "r") as f:
        content = f.read()
    lines = content.split('\n')
    out = []
    for line in lines:
        if line.strip().startswith('const val') and '=' not in line:
            continue
        out.append(line)
    
    with open(path, "w") as f:
        f.write('\n'.join(out))
        
fix_main()
fix_store()
print("Final Fixes applied.")
