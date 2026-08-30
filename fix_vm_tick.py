import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

old = """            launch {
                brokerManager.marketDataEngine.lastTickTimeFormatted.collect { time ->
                    if (time.isNotBlank()) {
                        _marketDataLastUpdated.value = time
                    }
                }
            }"""

new = """            launch {
                brokerManager.marketDataEngine.lastTickTimeMs.collect { time ->
                    if (time > 0) {
                        _marketDataLastUpdated.value = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(time))
                    }
                }
            }"""

content = content.replace(old, new)
with open(filepath, "w") as f:
    f.write(content)
