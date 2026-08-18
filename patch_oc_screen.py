import re

with open('/app/applet/app/src/main/java/com/example/ui/screens/OptionChainScreen.kt', 'r') as f:
    content = f.read()

# Make sure we don't duplicate OptionChainTabContent imports if they are already in the file
# If OptionChainTabContent is not imported, we need to import it since it's defined in IndexDetailsScreen.kt
if "import com.example.ui.screens.OptionChainTabContent" not in content and "import com.example.ui.screens.*" not in content:
    content = content.replace("import com.example.viewmodel.MainViewModel", "import com.example.viewmodel.MainViewModel\nimport com.example.ui.screens.OptionChainTabContent")
    with open('/app/applet/app/src/main/java/com/example/ui/screens/OptionChainScreen.kt', 'w') as f:
        f.write(content)
