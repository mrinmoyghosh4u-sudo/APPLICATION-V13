import os

filepath = "app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt"
with open(filepath, "r") as f:
    lines = f.readlines()

# Find where "if (showFyersWebView)" starts at the end
start_idx = -1
for i, line in enumerate(lines):
    if line.strip().startswith("if (showFyersWebView) {"):
        start_idx = i
        break

if start_idx != -1:
    webview_lines = lines[start_idx:]
    # Remove webview_lines from end
    lines = lines[:start_idx]
    
    # Find end of BrokerConnectDialog
    # It ends before @Composable private fun BrokerTab
    tab_idx = -1
    for i, line in enumerate(lines):
        if line.strip() == "private fun BrokerTab(":
            tab_idx = i
            break
            
    insert_idx = tab_idx - 2 # before @Composable
    while lines[insert_idx].strip() != "}":
        insert_idx -= 1
        
    # insert before the final brace of BrokerConnectDialog
    lines.insert(insert_idx, "".join(webview_lines))

with open(filepath, "w") as f:
    f.writelines(lines)
