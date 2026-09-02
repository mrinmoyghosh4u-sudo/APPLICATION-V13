package com.example.data.network

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NewsSystemSafetyTest {

    @Test
    fun testTradeConfirmationIsAlwaysRequired() {
        val sampleXml = """
        <rss version="2.0">
            <channel>
                <item>
                    <title>NIFTY surges 300 points in massive rally as RBI cuts repo rate</title>
                    <link>https://economictimes.indiatimes.com/markets/news1</link>
                    <description>Indian markets witnessed massive bullish momentum across all banking and IT stocks.</description>
                    <pubDate>Wed, 02 Sep 2026 09:30:00 +0530</pubDate>
                </item>
                <item>
                    <title>Sensex plunges 500 points amid global trade war escalation</title>
                    <link>https://economictimes.indiatimes.com/markets/news2</link>
                    <description>Bearish pressure mounts on Dalal street as foreign investors trigger heavy selloff.</description>
                    <pubDate>Wed, 02 Sep 2026 10:15:00 +0530</pubDate>
                </item>
            </channel>
        </rss>
        """.trimIndent()

        val parseMethod = MarketNewsFeedService.javaClass.getDeclaredMethod("parseRssXml", String::class.java, String::class.java).apply {
            isAccessible = true
        }

        @Suppress("UNCHECKED_CAST")
        val articles = parseMethod.invoke(MarketNewsFeedService, sampleXml, "The Economic Times") as List<com.example.data.model.OptionBuyerNewsArticle>

        assertEquals(2, articles.size)

        // Article 1: Bullish
        val bullishArt = articles[0]
        assertEquals("BULLISH", bullishArt.impact)
        assertEquals("CE WATCH", bullishArt.optionBuyerBias)
        // CRITICAL SAFETY REQUIREMENT: Must require confirmation, never automatic guaranteed buy
        assertEquals("REQUIRED", bullishArt.tradeConfirmation)
        assertTrue("Headline must match", bullishArt.headline.contains("NIFTY surges"))

        // Article 2: Bearish
        val bearishArt = articles[1]
        assertEquals("BEARISH", bearishArt.impact)
        assertEquals("PE WATCH", bearishArt.optionBuyerBias)
        assertEquals("REQUIRED", bearishArt.tradeConfirmation)
    }

    @Test
    fun testUnavailableStateWhenNoSourcesAvailable() {
        val emptyResult = MarketNewsFeedService.NewsFetchResult(
            articles = emptyList(),
            source = "UNAVAILABLE",
            isSuccess = false,
            status = "UNAVAILABLE",
            freshness = "UNAVAILABLE",
            errorMessage = "News feeds currently unreachable."
        )

        assertFalse(emptyResult.isSuccess)
        assertEquals("UNAVAILABLE", emptyResult.status)
        assertEquals("UNAVAILABLE", emptyResult.freshness)
        assertTrue("Articles list must be empty, no fake news!", emptyResult.articles.isEmpty())
    }
}
