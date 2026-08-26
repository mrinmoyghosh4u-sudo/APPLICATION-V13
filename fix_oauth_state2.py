import re

with open("app/src/main/java/com/example/viewmodel/MainViewModel.kt", "r") as f:
    content = f.read()

content = re.sub(
    r'            \} else if \(callbackState\.startsWith\("fyers_"\).*?sessionManager\.pendingOAuthSession = pendingSession\s*\}\s*\}',
    '',
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/viewmodel/MainViewModel.kt", "w") as f:
    f.write(content)

