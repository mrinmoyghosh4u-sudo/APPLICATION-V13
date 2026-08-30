import re

def patch_file(filepath, patterns_to_remove):
    try:
        with open(filepath, 'r') as f:
            content = f.read()
    except Exception as e:
        print(f"Skipping {filepath}: {e}")
        return

    original_content = content
    for pattern in patterns_to_remove:
        content = re.sub(pattern, '', content, flags=re.MULTILINE | re.DOTALL)

    if content != original_content:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Patched {filepath}")
    else:
        print(f"No changes made to {filepath}")

def line_re(pattern):
    return r'^[ \t]*' + pattern + r'.*\n'

# 4. SessionManager.kt
patch_file('app/src/main/java/com/example/data/network/SessionManager.kt', [
    r'// === m\.Stock ===\n',
    line_re(r'var mstockClientCode'),
    line_re(r'var mstockApiKey'),
    line_re(r'var mstockTotpSecret'),
    line_re(r'var mstockAccessToken'),
    line_re(r'var mstockTokenTimestamp'),
    line_re(r'var isMStockConnected'),
    line_re(r'fun saveMStockCredentials'),
    r'var mstockClientCode: String\?.*?(?=\n\s*(?:var|fun|//))',
    r'var mstockApiKey: String\?.*?(?=\n\s*(?:var|fun|//))',
    r'var mstockTotpSecret: String\?.*?(?=\n\s*(?:var|fun|//))',
    r'var mstockAccessToken: String\?.*?(?=\n\s*(?:var|fun|//))',
    r'var mstockTokenTimestamp: Long.*?(?=\n\s*(?:var|fun|//))',
    r'var isMStockConnected: Boolean.*?(?=\n\s*(?:var|fun|//))',
    r'fun saveMStockCredentials.*?\{.*?\n\s*\}\n',
    r'fun saveMStockToken.*?\{.*?\n\s*\}\n',
    r'fun clearMStockSessionTokens.*?\{.*?\n\s*\}\n',
    r'fun clearMStockCredentials.*?\{.*?\n\s*\}\n',
    r'fun isMStockConfigured.*?\{.*?\n\s*\}\n',
])

# 5. BrokerAuthManager.kt
patch_file('app/src/main/java/com/example/data/network/BrokerAuthManager.kt', [
    r'// ==========================================\n\s*// M\.STOCK.*?// ==========================================\n',
    r'private suspend fun validateMStockSession\(\).*?\{.*?\n\s*\}\n',
    r'private suspend fun processMStockLogin\(\).*?\{.*?\n\s*\}\n',
    r'suspend fun loginMStock\(\).*?\{.*?\n\s*\}\n',
    r'suspend fun refreshMStock\(\).*?\{.*?\n\s*\}\n',
    line_re(r'validateMStockSession\(\)'),
    line_re(r'val mstockAuthResult = brokerManager.mstockAuthManager.login'),
    line_re(r'updateStatus\("m\.Stock"'),
    line_re(r'sessionManager.clearMStock'),
    line_re(r'brokerManager.healthManager.report.*PROVIDER_MSTOCK'),
    line_re(r'brokerManager.mStockMarketDataService.connect\(\)'),
    line_re(r'getConnectionStatus\("m\.Stock"\)'),
])

