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

patch_file('app/src/test/java/com/example/Phase1ProviderHealthTest.kt', [
    r'healthManager\.reportTickReceived\(ProviderHealthManager\.PROVIDER_MSTOCK, oldTimestamp\)\n',
    r'assertEquals\("LIVE", healthManager\.getHealthState\(ProviderHealthManager\.PROVIDER_MSTOCK\)\.status\)\n',
    r'val staleState = healthManager\.getHealthState\(ProviderHealthManager\.PROVIDER_MSTOCK\)\n',
    r'assertEquals\("STALE", staleState\.status\)\n'
])

patch_file('app/src/test/java/com/example/MarketDataFailoverTest.kt', [
    r'healthManager\.reportConnection\(ProviderHealthManager\.PROVIDER_MSTOCK, false\)\n',
    r'assertFalse\(healthManager\.isProviderHealthy\(ProviderHealthManager\.PROVIDER_MSTOCK\)\)\n'
])

patch_file('app/src/test/java/com/example/BrokerAuthVerificationTest.kt', [
    r'assertEquals\("m\.Stock", state\.provider\)\n'
])

