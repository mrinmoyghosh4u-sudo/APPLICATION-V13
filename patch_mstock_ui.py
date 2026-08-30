import re

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


patch_file('app/src/main/java/com/example/util/diagnostic/SelfDiagnosticEngine.kt', [
    r'[ \t]*// 4\. Monitor m\.Stock \(Market Data Priority 4\)\n',
    r'[ \t]*val mStockHealth = checkBrokerHealth\("m\.STOCK"\)\n',
    r'[ \t]*newHealthMap\["MSTOCK"\] = mStockHealth\n',
])


patch_file('app/src/main/java/com/example/ui/screens/ProfileScreen.kt', [
    line_re(r'val isMStockConnected'),
    r'[ \t]*// 4\. m\.Stock Row \(Fallback #3 Market Data\)[\s\S]*?onRemoveAccountBroker\("m\.Stock"\)[ \t]*\}[ \t]*\)[ \t]*\n',
], [
    (r'isMStockConnected \|\| ', ''),
    (r'UPSTOX > FYERS > ANGEL > m\.STOCK', 'UPSTOX > FYERS > ANGEL'),
])

patch_file('app/src/main/java/com/example/ui/screens/DiagnosticsScreen.kt', [
    line_re(r'val mStockConnectionState'),
    line_re(r'val mStockHealth by'),
    line_re(r'mStockState = mStockConnectionState'),
    line_re(r'mStockHealth = mStockHealth'),
    r'(\s*)"m\.Stock" -> \{[\s\S]*?\}\n',
    line_re(r'mStockState: String,'),
    line_re(r'mStockHealth: String,'),
    line_re(r'val mStockHealthState = providerHealthMap\["m\.Stock"\]'),
    r'(\s*)MarketDataSourceRow\([\s\S]*?mStockHealthState\?\.subscriptionState \?= "UNSUBSCRIBED"\n\s*\)\n',
    # Ensure no leftover parenthesis or trailing commas issue
    r',?\s*mStockState\s*=\s*mStockConnectionState',
    r',?\s*mStockHealth\s*=\s*mStockHealth',
], [
    (r'"Upstox", "Fyers", "Angel One", "m\.Stock"', '"Upstox", "Fyers", "Angel One"'),
    (r'UPSTOX -> FYERS -> ANGEL ONE -> M\.STOCK', 'UPSTOX -> FYERS -> ANGEL ONE'),
])

patch_file('app/src/main/java/com/example/ui/screens/AISignalsScreen.kt', [], [
    (r'ANGEL ONE \(Fallback #1\), or m\.STOCK \(Fallback #2\)', 'ANGEL ONE (Fallback #1)')
])

patch_file('app/src/main/java/com/example/ui/screens/LoginScreen.kt', [
    r'(\s*)// --- M\.STOCK LOGIN BUTTON \(SECONDARY DATA\) ---[\s\S]*?onConnectBroker\("m\.Stock"\)[ \t]*\}[ \t]*\)[ \t]*\n',
])

patch_file('app/src/main/java/com/example/ui/components/CommonComponents.kt', [
    line_re(r'dataSource\.contains\("M\.STOCK"'),
])

patch_file('app/src/main/java/com/example/data/network/MarketDataEngine.kt', [], [
    (r'-> 4\. m\.Stock \(Fallback #3\)', ''),
])

