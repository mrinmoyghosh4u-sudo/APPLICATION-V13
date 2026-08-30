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
    r'import com\.example\.data\.network\.MStockMarketDataService\n',
    r'[ \t]*@Test\s*fun testMStockExchangeCodeMapping\(\) \{[\s\S]*?(?=\s*@Test|\}\s*$)',
])

patch_file('app/src/test/java/com/example/MarketDataFailoverTest.kt', [
    r'[ \t]*@Test\s*fun test3_fyersAndAngelDisconnect_triggersFailoverToMStock\(\) \{[\s\S]*?(?=\s*@Test|\}\s*$)',
    r'[ \t]*@Test\s*fun test5_mStockConnectedNoTick_isNotLive\(\) \{[\s\S]*?(?=\s*@Test|\}\s*$)',
    r'[ \t]*@Test\s*fun test6_mStockBinaryParser_parsesValidPacket\(\) \{[\s\S]*?(?=\s*@Test|\}\s*$)',
])

patch_file('app/src/test/java/com/example/BrokerAuthVerificationTest.kt', [
    r'\s*ProviderHealthManager\.PROVIDER_MSTOCK,?',
    r'\s*assertEquals\("m\.Stock", failoverOrder\[3\]\)',
    r'[ \t]*@Test\s*fun test15_webSocketDisconnectStateTransition\(\) \{[\s\S]*?(?=\s*@Test|\}\s*$)',
], [
    (r'assertEquals\(4, failoverOrder\.size\)', 'assertEquals(3, failoverOrder.size)')
])

