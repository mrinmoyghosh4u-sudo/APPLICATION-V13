import re

with open("app/src/main/java/com/example/data/network/MarketIntelligenceService.kt", "r") as f:
    content = f.read()

# Replace global cues list
start = content.find('val globalCues = listOf(')
end = content.find('// 5. FII / DII INSTITUTIONAL CASH FLOW')
if start != -1 and end != -1:
    content = content[:start] + 'val globalCues = emptyList<GlobalCueItem>()\n            ' + content[end:]

# Replace FiiDiiFlowData
start = content.find('val fiiDiiData = FiiDiiFlowData(')
end = content.find('// 6. PRE-MARKET LEVELS & S/R CALCULATION')
if start != -1 and end != -1:
    content = content[:start] + '''val fiiDiiData = FiiDiiFlowData(
                fiiBuy = 0.0,
                fiiSell = 0.0,
                fiiNet = 0.0,
                diiBuy = 0.0,
                diiSell = 0.0,
                diiNet = 0.0,
                totalNet = 0.0,
                institutionalBias = "DATA UNAVAILABLE",
                dateFormatted = "Awaiting live data",
                source = "DATA UNAVAILABLE"
            )
            ''' + content[end:]

# Replace buildMarketNewsFeed
content = re.sub(r'private fun buildMarketNewsFeed.*?return listOf\([^]]+\]\n    \}', 'private fun buildMarketNewsFeed(niftyLtp: Double, bankNiftyLtp: Double): List<OptionBuyerNewsArticle> {\n        return emptyList()\n    }', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/data/network/MarketIntelligenceService.kt", "w") as f:
    f.write(content)
