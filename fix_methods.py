import re

content = open('app/src/main/java/com/example/data/network/AngelOneBrokerService.kt').read()

def fix_method(name, content):
    pattern = rf"(override suspend fun {name}\(\)[^{{]*{{)(.*?)(?=override suspend fun|\Z)"
    match = re.search(pattern, content, flags=re.DOTALL)
    if match:
        body = match.group(2)
        # Find where it maps or does something and then incorrectly ends
        if "emptyList()" in body and "map {" in body:
            # We want to replace `emptyList()` at the end with `} ?: emptyList()` or `} else { emptyList() }`
            pass # Too complex for simple regex, I'll just write the full file content manually!

