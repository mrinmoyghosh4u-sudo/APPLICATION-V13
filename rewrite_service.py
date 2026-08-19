import re

content = open('app/src/main/java/com/example/data/network/AngelOneBrokerService.kt').read()

# Replace getRMS
content = re.sub(
r'''    override suspend fun getRMS\(\): Result<Double> \{
        if \(sessionManager.angelJwtToken.isNullOrEmpty\(\)\) \{
            return Result.success\(0.0\)
        return runCatching \{
            val response = runCatching \{ api.getRMS\(\) \}.getOrNull\(\)
            if \(response != null && response.isSuccessful && response.body\(\)\?.status == true\) \{
                response.body\(\)\?.data\?.availableMargin.toDoubleOrDefault\(0.0\)
                0.0
    \}''',
'''    override suspend fun getRMS(): Result<Double> {
        if (sessionManager.angelJwtToken.isNullOrEmpty()) return Result.success(0.0)
        return runCatching {
            val response = runCatching { api.getRMS() }.getOrNull()
            if (response != null && response.isSuccessful && response.body()?.status == true) {
                response.body()?.data?.availableMargin.toDoubleOrDefault(0.0)
            } else {
                0.0
            }
        }
    }''', content)

# Replace getOrders
content = re.sub(
r'''    override suspend fun getOrders\(\): Result<List<OrderEntity>> \{
        if \(sessionManager.angelJwtToken.isNullOrEmpty\(\)\) \{
            return Result.success\(emptyList\(\)\)
        return runCatching \{
            val response = runCatching \{ api.getOrderBook\(\) \}.getOrNull\(\)
            if \(response != null && response.isSuccessful && response.body\(\)\?.status == true\) \{
                val items = response.body\(\)\?.data \?: emptyList\(\)
                items.map \{ item ->(.*?)emptyList\(\)
    \}''',
'''    override suspend fun getOrders(): Result<List<OrderEntity>> {
        if (sessionManager.angelJwtToken.isNullOrEmpty()) return Result.success(emptyList())
        return runCatching {
            val response = runCatching { api.getOrderBook() }.getOrNull()
            if (response != null && response.isSuccessful && response.body()?.status == true) {
                val items = response.body()?.data ?: emptyList()
                items.map { item ->\g<1>                }
            } else {
                emptyList()
            }
        }
    }''', content, flags=re.DOTALL)

# Replace getHoldings
content = re.sub(
r'''    override suspend fun getHoldings\(\): Result<List<PortfolioHoldingEntity>> \{
        if \(sessionManager.angelJwtToken.isNullOrEmpty\(\)\) \{
            return Result.success\(emptyList\(\)\)
        return runCatching \{
            val response = runCatching \{ api.getHoldings\(\) \}.getOrNull\(\)
            if \(response != null && response.isSuccessful && response.body\(\)\?.status == true\) \{
                val items = response.body\(\)\?.data \?: emptyList\(\)
                items.map \{ item ->(.*?)emptyList\(\)
    \}

            \}
        \}''',
'''    override suspend fun getHoldings(): Result<List<PortfolioHoldingEntity>> {
        if (sessionManager.angelJwtToken.isNullOrEmpty()) return Result.success(emptyList())
        return runCatching {
            val response = runCatching { api.getHoldings() }.getOrNull()
            if (response != null && response.isSuccessful && response.body()?.status == true) {
                val items = response.body()?.data ?: emptyList()
                items.map { item ->\g<1>                }
            } else {
                emptyList()
            }
        }
    }''', content, flags=re.DOTALL)

# Replace getPositions
content = re.sub(
r'''    override suspend fun getPositions\(\): Result<List<PortfolioHoldingEntity>> \{
        if \(sessionManager.angelJwtToken.isNullOrEmpty\(\)\) \{
            return Result.success\(emptyList\(\)\)
        return runCatching \{
            val response = runCatching \{ api.getPositions\(\) \}.getOrNull\(\)
            if \(response != null && response.isSuccessful && response.body\(\)\?.status == true\) \{
                val items = response.body\(\)\?.data \?: emptyList\(\)
                items.map \{ item ->(.*?)emptyList\(\)
    \}

            \}
        \}''',
'''    override suspend fun getPositions(): Result<List<PortfolioHoldingEntity>> {
        if (sessionManager.angelJwtToken.isNullOrEmpty()) return Result.success(emptyList())
        return runCatching {
            val response = runCatching { api.getPositions() }.getOrNull()
            if (response != null && response.isSuccessful && response.body()?.status == true) {
                val items = response.body()?.data ?: emptyList()
                items.map { item ->\g<1>                }
            } else {
                emptyList()
            }
        }
    }''', content, flags=re.DOTALL)

open('app/src/main/java/com/example/data/network/AngelOneBrokerService.kt', 'w').write(content)

