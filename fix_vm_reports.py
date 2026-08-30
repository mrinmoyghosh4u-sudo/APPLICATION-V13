import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

props = """
    private fun reportAuthenticating(provider: String) {}
    private fun reportWaitingForCallback(provider: String, state: String) {}
    private fun reportAuthFailure(provider: String, message: String) {}
    private fun reportCallbackReceived(provider: String, state: String) {}
    private fun reportAuthCodeReceived(provider: String, code: String) {}
"""

idx = content.find("class MainViewModel")
if idx != -1:
    brace_idx = content.find("{", idx)
    if brace_idx != -1:
        content = content[:brace_idx+1] + props + content[brace_idx+1:]
        with open(filepath, "w") as f:
            f.write(content)
