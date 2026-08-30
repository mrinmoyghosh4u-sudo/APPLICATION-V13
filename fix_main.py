import re
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Fix MainActivity UI block bracket
content = re.sub(r'onFyersLogin = \{ app, secret, codeOrToken -> viewModel\.connectFyers\(app, secret, codeOrToken\) \}[\s\S]*?$', 'onFyersLogin = { app, secret, codeOrToken -> viewModel.connectFyers(app, secret, codeOrToken) }\n                    )\n                }\n            }\n        }\n    }\n}', content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
