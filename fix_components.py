import re
import glob
for file in glob.glob("app/src/main/java/com/example/ui/**/*.kt", recursive=True):
    try:
        with open(file, "r") as f:
            content = f.read()
        content = content.replace("DarkCardBackground", "androidx.compose.ui.graphics.Color(0xFF1E1E1E)")
        with open(file, "w") as f:
            f.write(content)
    except Exception:
        pass
