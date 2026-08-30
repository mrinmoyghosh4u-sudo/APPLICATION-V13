import re

filepath = "app/src/main/java/com/example/util/AlgoEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace('storeTick.state == "STALE" || storeTick.state == "OFFLINE"', 'providerState.stale || !providerState.live')
content = content.replace('storeTick.state', 'if(providerState.stale) "STALE" else "OFFLINE"')

with open(filepath, "w") as f:
    f.write(content)
