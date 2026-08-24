import os

filepath = "app/src/main/java/com/example/ui/components/CommonComponents.kt"
with open(filepath, "r") as f:
    content = f.read()

old_sig = """fun LiveStatusBadge(
    isLive: Boolean,"""
new_sig = """fun LiveStatusBadge(
    isLive: Boolean,
    dataSource: String = "",
"""

if "dataSource: String" not in content:
    content = content.replace(old_sig, new_sig)
    
    old_text = 'text = if (isLive) "LIVE" else "OFFLINE",'
    new_text = 'text = if (isLive) "LIVE${if(dataSource.isNotBlank()) " ● $dataSource" else ""}" else "OFFLINE${if(dataSource.isNotBlank()) " ● $dataSource" else ""}",'
    
    content = content.replace(old_text, new_text)

    with open(filepath, "w") as f:
        f.write(content)

