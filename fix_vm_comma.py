import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("redirectUri, )", "redirectUri)")
content = content.replace("cleanAppId, redirectUri, )", "cleanAppId, redirectUri)")
content = content.replace("cleanClientId, redirectUri, )", "cleanClientId, redirectUri)")

with open(filepath, "w") as f:
    f.write(content)
