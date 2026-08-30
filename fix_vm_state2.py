import re

filepath = "app/src/main/java/com/example/viewmodel/MainViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

props = """
    data class PendingOAuthSession(
        val provider: String = "",
        val state: String = "",
        val createdAt: Long = 0L,
        val consumed: Boolean = false
    )

    private val _pendingOAuthSession = MutableStateFlow<PendingOAuthSession?>(null)
    val pendingOAuthSession: StateFlow<PendingOAuthSession?> = _pendingOAuthSession.asStateFlow()

    private val _pendingFyersOAuthState = MutableStateFlow("")
    val pendingFyersOAuthState: StateFlow<String> = _pendingFyersOAuthState.asStateFlow()

    private val _pendingOAuthState = MutableStateFlow("")
    val pendingOAuthState: StateFlow<String> = _pendingOAuthState.asStateFlow()

    private var lastReceivedOAuthCode: String = ""
    private var lastReceivedOAuthTime: Long = 0L
"""

# Let's see where to inject this.
idx = content.find("class MainViewModel")
if idx != -1:
    brace_idx = content.find("{", idx)
    if brace_idx != -1:
        content = content[:brace_idx+1] + props + content[brace_idx+1:]
        with open(filepath, "w") as f:
            f.write(content)
