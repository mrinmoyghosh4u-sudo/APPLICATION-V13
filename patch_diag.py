import os
import re

filepath = "/app/applet/app/src/main/java/com/example/ui/screens/DiagnosticsScreen.kt"

with open(filepath, "r") as f:
    content = f.read()

content = re.sub(r'val nseHealth.*?\n', '', content)
content = re.sub(r'val nseState.*?\n', '', content)
content = re.sub(r'val yahooHealth.*?\n', '', content)
content = re.sub(r'DiagnosticItem\("NSE Data Role".*?\n', '', content)
content = re.sub(r'DiagnosticItem\("NSE Health".*?\n', '', content)
content = re.sub(r'DiagnosticItem\("Yahoo Data Role".*?\n', '', content)
content = re.sub(r'DiagnosticItem\("Yahoo Health".*?\n', '', content)

with open(filepath, "w") as f:
    f.write(content)
