import re
import sys

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

# 1. MarketDataStore.kt
patch_file('app/src/main/java/com/example/data/model/MarketDataStore.kt', [
    line_re(r'val mStockHealth'),
    line_re(r'_mStockHealth\.value'),
    r'val _mStockHealth.*?\n',
    r'MSTOCK,?\s*\n?',
    r'MarketDataSourceNames\.MSTOCK\s*->\s*_mStockHealth\.value\s*=\s*state\n'
])

# 2. ProviderHealthManager.kt
patch_file('app/src/main/java/com/example/data/network/ProviderHealthManager.kt', [
    line_re(r'const val PROVIDER_MSTOCK'),
    line_re(r'PROVIDER_MSTOCK\s*->')
])

# 3. MarketDataEngine.kt
patch_file('app/src/main/java/com/example/data/network/MarketDataEngine.kt', [
    r'import com\.example\.data\.network\.MStockMarketDataService\n',
    line_re(r'val mStockMarketDataService'),
    line_re(r'mStockMarketDataService\s*='),
    r'mStockMarketDataService\.subscribeToMarketData\(symbols\)\s*\n?',
    r'mStockMarketDataService\.isConnectionLive\(\).*?\|\|\s*',
    r'mStockMarketDataService\.hasFirstTickReceived\(\).*?\|\|\s*',
    line_re(r'ProviderHealthManager\.PROVIDER_MSTOCK'),
    line_re(r'mStockMarketDataService\.disconnect\(\)'),
    line_re(r'if \(mStockMarketDataService\.isConnectionLive\(\)\)'),
    line_re(r'mStockMarketDataService\.connect\(\)'),
    # Failover sequence checks
    r'if \(healthManager\.isProviderHealthy\(ProviderHealthManager\.PROVIDER_MSTOCK\)\) \{\n.*?\} else \{\n.*?\n\s*\}\n',
    r'else if \(healthManager\.isProviderHealthy\(ProviderHealthManager\.PROVIDER_MSTOCK\)\) \{\n.*?\}\s*\n',
    r'if \(healthManager\.isProviderHealthy\(ProviderHealthManager\.PROVIDER_MSTOCK\)\) \{\s*Log\.i\(TAG, "\[FAILOVER\] Primary & Fallback down\. Activating m\.Stock Tertiary\."\)\s*\} else \{\s*Log\.e\(TAG, "\[FAILOVER_EXHAUSTED\] All data providers are down!"\)\s*\}\s*\n?',
])

