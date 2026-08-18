import re

with open('/app/applet/app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'r') as f:
    content = f.read()

pattern = r'    private fun fetchOptionChain\(\) \{\s*viewModelScope.launch \{\s*val res = brokerManager.getOptionChain\(_selectedOptionIndex.value, _selectedOptionExpiry.value\)\s*res.getOrNull\(\)\?\.let \{ _optionStrikes.value = it \}\s*\}\s*\}'

replacement = """    private fun fetchOptionChain() {
        viewModelScope.launch {
            _optionStrikes.value = emptyList() // clear previous
            val res = brokerManager.getOptionChain(_selectedOptionIndex.value, _selectedOptionExpiry.value)
            _optionStrikes.value = res.getOrNull() ?: emptyList()
        }
    }"""

new_content = re.sub(pattern, replacement, content)

if new_content != content:
    with open('/app/applet/app/src/main/java/com/example/viewmodel/MainViewModel.kt', 'w') as f:
        f.write(new_content)
    print("MainViewModel Patched!")
else:
    print("MainViewModel Patch failed!")

