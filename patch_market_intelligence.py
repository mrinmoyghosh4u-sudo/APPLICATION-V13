import re

with open("app/src/main/java/com/example/data/network/MarketIntelligenceService.kt", "r") as f:
    content = f.read()

# Replace Global Cues with empty
content = re.sub(r'val globalCues = listOf\([^)]+\)', 'val globalCues = emptyList<GlobalCueItem>()', content, flags=re.DOTALL)

# Replace FiiDiiFlowData with Unavailable
fii_dii_replacement = """val fiiDiiData = FiiDiiFlowData(
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
            )"""
content = re.sub(r'val fiiDiiData = FiiDiiFlowData\([^)]+\)', fii_dii_replacement, content, flags=re.DOTALL)

# Replace buildMarketNewsFeed to return empty
content = re.sub(r'private fun buildMarketNewsFeed.*?return listOf\([^]]+\]\n    \}', 'private fun buildMarketNewsFeed(niftyLtp: Double, bankNiftyLtp: Double): List<OptionBuyerNewsArticle> {\n        return emptyList()\n    }', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/data/network/MarketIntelligenceService.kt", "w") as f:
    f.write(content)
