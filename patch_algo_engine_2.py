import re

with open('app/src/main/java/com/example/util/AlgoEngine.kt', 'r') as f:
    content = f.read()

# Remove executeAlgoOrderIfNeeded function entirely
content = re.sub(r'\s*private fun executeAlgoOrderIfNeeded\(signal: AISignalEntity\) \{.*?(?=\n\s*private fun updateActivePositionsPnl|\n\s*fun exitPaperPosition)', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/util/AlgoEngine.kt', 'w') as f:
    f.write(content)
