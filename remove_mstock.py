import re

with open("app/src/main/java/com/example/data/network/MarketDataEngine.kt", "r") as f:
    content = f.read()

# Remove mStockMarketDataService from constructor
content = re.sub(r'val mStockMarketDataService: MStockMarketDataService,\s*', '', content)

# Remove mstock failover blocks
content = re.sub(r'healthManager\.logFailover\(ProviderHealthManager\.PROVIDER_ANGEL_ONE, ProviderHealthManager\.PROVIDER_MSTOCK\)\s*if \(mStockMarketDataService.*?healthManager\.reportError\(ProviderHealthManager\.PROVIDER_MSTOCK\)\s*\}', '', content, flags=re.DOTALL)
content = re.sub(r'if \(mStockMarketDataService.*?healthManager\.reportError\(ProviderHealthManager\.PROVIDER_MSTOCK\)\s*\}', '', content, flags=re.DOTALL)
content = re.sub(r'if \(mStockMarketDataService\.isConfigured\(\)\)\s*\{\s*mStockMarketDataService\.reconnect\(\)\s*\}', '', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/data/network/MarketDataEngine.kt", "w") as f:
    f.write(content)

