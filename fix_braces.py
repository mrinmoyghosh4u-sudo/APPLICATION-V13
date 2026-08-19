lines = open('app/src/main/java/com/example/data/network/AngelOneBrokerService.kt').read().split('\n')
out = []
for line in lines:
    out.append(line)
    if "override suspend fun getPositions" in line or "override suspend fun getMarketQuotes" in line:
        out.insert(-1, "            }")
        out.insert(-1, "        }")
open('app/src/main/java/com/example/data/network/AngelOneBrokerService.kt', 'w').write('\n'.join(out))
