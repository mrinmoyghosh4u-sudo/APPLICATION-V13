import re

filepath = "app/src/main/java/com/example/data/network/ProviderHealthManager.kt"
with open(filepath, "r") as f:
    content = f.read()

props = """
        const val STATE_CREDENTIALS_MISSING = "CREDENTIALS_MISSING"
"""
idx = content.find("companion object {")
if idx != -1:
    brace_idx = content.find("{", idx)
    if brace_idx != -1:
        content = content[:brace_idx+1] + props + content[brace_idx+1:]
        with open(filepath, "w") as f:
            f.write(content)
