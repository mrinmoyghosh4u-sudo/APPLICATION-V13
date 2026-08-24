import os
import re

filepath = "/app/applet/app/src/main/java/com/example/data/network/MarketDataEngine.kt"

with open(filepath, "r") as f:
    content = f.read()

replacement = """
    private val _lastTickTimeMs = MutableStateFlow(0L)
    val lastTickTimeMs: StateFlow<Long> = _lastTickTimeMs.asStateFlow()
    
    private val _lastTickTimeFormatted = MutableStateFlow("--")
    val lastTickTimeFormatted: StateFlow<String> = _lastTickTimeFormatted.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
"""

content = content.replace(
"""
    private val _lastTickTimeMs = MutableStateFlow(0L)
    val lastTickTimeMs: StateFlow<Long> = _lastTickTimeMs.asStateFlow()
""", replacement)

content = content.replace("updateLastTickTime() {", 
"""updateLastTickTime() {
        val now = System.currentTimeMillis()
        _lastTickTimeFormatted.value = timeFormat.format(Date(now))
""")

with open(filepath, "w") as f:
    f.write(content)
