import re

filepath = "app/src/main/java/com/example/data/network/SessionManager.kt"
with open(filepath, "r") as f:
    content = f.read()

props = """
    fun markDhanTokenIdConsumed(token: String) {
        // No-op for now
    }
"""
idx = content.rfind("}")
if idx != -1:
    content = content[:idx] + props + content[idx:]
    with open(filepath, "w") as f:
        f.write(content)
