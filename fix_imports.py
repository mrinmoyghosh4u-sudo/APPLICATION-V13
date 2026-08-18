import re

with open('app/src/main/java/com/example/ui/screens/LoginScreen.kt', 'r') as f:
    content = f.read()

imports_to_add = """
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.outlined.*
"""

if "ContentScale" not in content[:content.find("@Composable")]:
    content = re.sub(r'import androidx.compose.ui.Modifier', imports_to_add + '\\nimport androidx.compose.ui.Modifier', content)

with open('app/src/main/java/com/example/ui/screens/LoginScreen.kt', 'w') as f:
    f.write(content)
