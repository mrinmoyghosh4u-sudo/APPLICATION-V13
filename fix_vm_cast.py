import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("val onlyWithLtp = combinedList.filter { it.ltp > 0.0 }", "val onlyWithLtp = (combinedList as List<com.example.data.model.WatchlistItem>).filter { it.ltp > 0.0 }")
with open(filepath, "w") as f:
    f.write(content)
