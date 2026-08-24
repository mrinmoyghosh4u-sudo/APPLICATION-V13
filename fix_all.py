import os

# 1. MainViewModel.kt imports
filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

if "import kotlinx.coroutines.flow.stateIn" not in content:
    content = content.replace("import kotlinx.coroutines.flow.map", "import kotlinx.coroutines.flow.map\nimport kotlinx.coroutines.flow.stateIn\nimport kotlinx.coroutines.flow.SharingStarted")
    with open(filepath, "w") as f:
        f.write(content)

