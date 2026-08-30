filepath = "app/src/main/java/com/example/data/network/MarketDataEngine.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("previousClose = (item.ltp - item.change),,", "previousClose = (item.ltp - item.change),")
with open(filepath, "w") as f:
    f.write(content)

filepath_vm = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath_vm, "r") as f:
    content_vm = f.read()

content_vm = content_vm.replace(
"""            if (true) {
                _optionStrikes.value = strikes
            } else {""",
"""            if (strikes != null) {
                _optionStrikes.value = strikes
            } else {"""
)
with open(filepath_vm, "w") as f:
    f.write(content_vm)
