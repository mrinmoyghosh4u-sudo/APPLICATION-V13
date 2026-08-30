import re
import sys

def patch_file(filepath, patterns_to_remove, patterns_to_replace=None):
    try:
        with open(filepath, 'r') as f:
            content = f.read()
    except Exception as e:
        print(f"Skipping {filepath}: {e}")
        return

    original_content = content
    for pattern in patterns_to_remove:
        content = re.sub(pattern, '', content, flags=re.MULTILINE | re.DOTALL)

    if patterns_to_replace:
        for p, r in patterns_to_replace:
            content = re.sub(p, r, content, flags=re.MULTILINE | re.DOTALL)

    if content != original_content:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Patched {filepath}")
    else:
        print(f"No changes made to {filepath}")

def line_re(pattern):
    return r'^[ \t]*' + pattern + r'.*\n'

# 6. BrokerConnectDialog.kt
patch_file('app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt', [
    line_re(r'onMStockLogin'),
    line_re(r'var mstockClientId'),
    line_re(r'var mstockApiKey'),
    line_re(r'var mstockTotpSecret'),
    r'(\s*)"m\.Stock" -> \{\s*val brokerHealth = providerHealth\[ProviderHealthManager\.PROVIDER_MSTOCK\].*?\} // End m\.Stock Tab\n',
    # Note: Using a safer regex for the m.Stock tab removal:
    r'^[ \t]*"m\.Stock" -> \{.*?(?:^[ \t]*\}(?=[ \t]*\n[ \t]*"Dhan" ->|\n[ \t]*\} // End content)|// End m\.Stock Tab\n)',
    r'BrokerTabs\([^)]*listOf\("Dhan", "Upstox", "Fyers", "Angel One", "m\.Stock"\)[^)]*\)',
], [
    (r'listOf\("Dhan", "Upstox", "Fyers", "Angel One", "m\.Stock"\)', 'listOf("Dhan", "Upstox", "Fyers", "Angel One")'),
    (r'"m\.Stock" -> \{[\s\S]*?(?=\s*"[A-Za-z ]+" -> \{|\s*\}\s*$)', '')  # Removing the whole tab more aggressively if needed
])

# 7. BrokerType.kt
patch_file('app/src/main/java/com/example/data/network/BrokerType.kt', [
    r'MSTOCK\([^)]*\),?\s*\n*',
    r'trimmed\.equals\("m\.Stock".*?-> MSTOCK\n',
    r'\* 4\. MSTOCK.*?FEED \(Mirae Asset\)\n',
])

# 8. BrokerAuthManager.kt (extra cleanup)
patch_file('app/src/main/java/com/example/data/network/BrokerAuthManager.kt', [
    line_re(r'import com\.example\.util\.MStockAuthHelper'),
    line_re(r'private val mStockMarketDataService'),
    line_re(r'"m\.Stock" to BrokerConnectionState'),
    line_re(r'com\.example\.data\.model\.MarketDataStore\.mStockHealth\.collect'),
    line_re(r'val current = _statuses\.value\["m\.Stock"\]'),
    r'com\.example\.data\.model\.MarketDataStore\.mStockHealth\.collect \{ health ->[\s\S]*?\}\n',
    r'\* 5\. m\.Stock -> FALLBACK #3 MARKET DATA FEED\n',
    line_re(r'suspend fun connectMStock'),
])

# 9. BrokerManager.kt
patch_file('app/src/main/java/com/example/data/network/BrokerManager.kt', [
    line_re(r'val mStockMarketDataService = MStockMarketDataService'),
    line_re(r'mStockMarketDataService = mStockMarketDataService'),
    r' \* - Market Data: Unified MarketDataEngine manages hidden automatic failover across Fyers, Angel One, m\.Stock\.\n',
], [
    (r'\* - Market Data: Unified MarketDataEngine manages hidden automatic failover across Fyers, Angel One, m\.Stock\.',
     '* - Market Data: Unified MarketDataEngine manages hidden automatic failover across Fyers, Angel One.'),
])

# 10. InstrumentResolvers.kt
patch_file('app/src/main/java/com/example/data/network/InstrumentResolvers.kt', [
    r'class MStockInstrumentResolver.*?\}\n',
])

# 11. MarketDataStore.kt (extra)
patch_file('app/src/main/java/com/example/data/model/MarketDataStore.kt', [
    line_re(r'const val.*?= "mStock"'),
    r'\* - m\.Stock = Fallback #3\n',
])

# 12. MainViewModel.kt
patch_file('app/src/main/java/com/example/viewmodel/MainViewModel.kt', [
    r'BrokerType\.MSTOCK -> \{[\s\S]*?(?=\s*BrokerType\.)',
    r'fun connectMStock\([\s\S]*?(?=\s*fun|\s*\n\s*\} // End ViewModel)',
])

# 13. MainActivity.kt
patch_file('app/src/main/java/com/example/MainActivity.kt', [
    line_re(r'onMStockLogin ='),
])

