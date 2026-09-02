package com.example.data.network

import android.util.Log
import android.util.Xml
import com.example.data.model.OptionBuyerNewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Real-Time Market News & Intelligence Aggregator for KING KHAN AI TRADER
 *
 * Architecture:
 * 1. Primary: Remote Backend Aggregator Proxy (Vercel serverless /api/news)
 * 2. Fallback: Direct Public Indian Financial RSS Feeds (The Economic Times, Moneycontrol, Livemint)
 * 3. Never returns fake / mock / hardcoded articles.
 * 4. If network fails or no source reachable: returns emptyList() and sets status to UNAVAILABLE.
 */
object MarketNewsFeedService {
    private const val TAG = "MarketNewsFeedService"
    private const val BACKEND_NEWS_URL = "https://application-beige-psi.vercel.app/api/news"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val RSS_FALLBACK_FEEDS = listOf(
        Pair("The Economic Times", "https://economictimes.indiatimes.com/markets/rssfeeds/1977021501.cms"),
        Pair("Moneycontrol", "https://www.moneycontrol.com/rss/marketreports.xml"),
        Pair("Moneycontrol Top News", "https://www.moneycontrol.com/rss/MCtopnews.xml"),
        Pair("Livemint", "https://www.livemint.com/rss/markets")
    )

    data class NewsFetchResult(
        val articles: List<OptionBuyerNewsArticle>,
        val source: String,
        val isSuccess: Boolean,
        val status: String,
        val errorMessage: String? = null
    )

    suspend fun fetchMarketNews(
        niftyLtp: Double = 0.0,
        bankNiftyLtp: Double = 0.0
    ): NewsFetchResult = withContext(Dispatchers.IO) {
        // Step 1: Try Remote Backend Proxy (Primary)
        try {
            val backendResult = fetchFromBackendProxy()
            if (backendResult.isSuccess && backendResult.articles.isNotEmpty()) {
                Log.i(TAG, "Successfully fetched ${backendResult.articles.size} news articles from backend proxy")
                return@withContext backendResult
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend news proxy fetch failed, falling back to direct RSS: ${e.message}")
        }

        // Step 2: Fallback to Direct Public RSS Feeds
        try {
            val rssResult = fetchFromDirectRssFeeds()
            if (rssResult.isSuccess && rssResult.articles.isNotEmpty()) {
                Log.i(TAG, "Successfully fetched ${rssResult.articles.size} news articles from direct RSS")
                return@withContext rssResult
            }
        } catch (e: Exception) {
            Log.e(TAG, "Direct RSS news fetch failed: ${e.message}", e)
        }

        // Step 3: Failure State (No fake news!)
        return@withContext NewsFetchResult(
            articles = emptyList(),
            source = "UNAVAILABLE",
            isSuccess = false,
            status = "UNAVAILABLE",
            errorMessage = "No active news feeds reachable. Check internet connection."
        )
    }

    private fun fetchFromBackendProxy(): NewsFetchResult {
        val request = Request.Builder()
            .url(BACKEND_NEWS_URL)
            .header("User-Agent", "KingKhanAiTrader/1.0 (Android)")
            .header("Accept", "application/json")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return NewsFetchResult(emptyList(), "Backend Proxy", false, "HTTP_${response.code}", "HTTP error ${response.code}")
            }

            val body = response.body?.string() ?: return NewsFetchResult(emptyList(), "Backend Proxy", false, "EMPTY_BODY")
            val json = JSONObject(body)
            val status = json.optString("status", "")
            if (status != "success") {
                return NewsFetchResult(emptyList(), "Backend Proxy", false, "ERROR", json.optString("message", "Unknown error"))
            }

            val articlesArray = json.optJSONArray("articles") ?: return NewsFetchResult(emptyList(), "Backend Proxy", false, "NO_ARTICLES")
            val articles = mutableListOf<OptionBuyerNewsArticle>()

            for (i in 0 until articlesArray.length()) {
                val item = articlesArray.getJSONObject(i)
                articles.add(
                    OptionBuyerNewsArticle(
                        id = item.optString("id", "news_$i"),
                        headline = item.optString("headline", ""),
                        source = item.optString("source", "Market Wire"),
                        publishedTime = item.optString("publishedTime", "Today"),
                        summary = item.optString("summary", ""),
                        category = item.optString("category", "ALL"),
                        isBreaking = item.optBoolean("isBreaking", false),
                        affectedMarket = item.optString("affectedMarket", "NIFTY 50"),
                        impact = item.optString("impact", "NEUTRAL"),
                        impactStrength = item.optString("impactStrength", "MEDIUM"),
                        optionBuyerBias = item.optString("optionBuyerBias", "WAIT"),
                        confidencePercent = item.optInt("confidencePercent", 75),
                        impactReason = item.optString("impactReason", "Watch for price confirmation"),
                        url = if (item.has("url") && !item.isNull("url")) item.optString("url") else null
                    )
                )
            }

            return NewsFetchResult(
                articles = articles,
                source = json.optString("source", "Aggregated Financial Proxy"),
                isSuccess = true,
                status = "HEALTHY"
            )
        }
    }

    private fun fetchFromDirectRssFeeds(): NewsFetchResult {
        val allArticles = mutableListOf<OptionBuyerNewsArticle>()
        val successfulSources = mutableListOf<String>()

        for ((sourceName, feedUrl) in RSS_FALLBACK_FEEDS) {
            try {
                val request = Request.Builder()
                    .url(feedUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) KingKhanTrader/1.0")
                    .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val xmlData = response.body?.string()
                        if (!xmlData.isNullOrBlank()) {
                            val parsed = parseRssXml(xmlData, sourceName)
                            if (parsed.isNotEmpty()) {
                                allArticles.addAll(parsed)
                                successfulSources.add(sourceName)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse RSS from $sourceName: ${e.message}")
            }
        }

        // Deduplicate articles by normalized title
        val seenTitles = mutableSetOf<String>()
        val uniqueArticles = mutableListOf<OptionBuyerNewsArticle>()

        for (article in allArticles) {
            val key = article.headline.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "").take(35)
            if (key.isNotEmpty() && !seenTitles.contains(key)) {
                seenTitles.add(key)
                uniqueArticles.add(article)
            }
        }

        return if (uniqueArticles.isNotEmpty()) {
            NewsFetchResult(
                articles = uniqueArticles,
                source = "Direct RSS (${successfulSources.joinToString(", ")})",
                isSuccess = true,
                status = "HEALTHY"
            )
        } else {
            NewsFetchResult(
                articles = emptyList(),
                source = "Direct RSS",
                isSuccess = false,
                status = "UNAVAILABLE",
                errorMessage = "All direct RSS feeds failed"
            )
        }
    }

    private fun parseRssXml(xml: String, sourceName: String): List<OptionBuyerNewsArticle> {
        val articles = mutableListOf<OptionBuyerNewsArticle>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var insideItem = false
            var currentTitle = ""
            var currentLink = ""
            var currentDesc = ""
            var currentPubDate = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("item", ignoreCase = true) || tagName.equals("entry", ignoreCase = true)) {
                            insideItem = true
                            currentTitle = ""
                            currentLink = ""
                            currentDesc = ""
                            currentPubDate = ""
                        } else if (insideItem) {
                            when {
                                tagName.equals("title", ignoreCase = true) -> {
                                    currentTitle = cleanXmlText(parser.nextText())
                                }
                                tagName.equals("link", ignoreCase = true) -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    currentLink = if (!href.isNullOrBlank()) href else cleanXmlText(parser.nextText())
                                }
                                tagName.equals("description", ignoreCase = true) || tagName.equals("summary", ignoreCase = true) -> {
                                    currentDesc = cleanXmlText(parser.nextText())
                                }
                                tagName.equals("pubDate", ignoreCase = true) || tagName.equals("published", ignoreCase = true) || tagName.equals("updated", ignoreCase = true) -> {
                                    currentPubDate = cleanXmlText(parser.nextText())
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("item", ignoreCase = true) || tagName.equals("entry", ignoreCase = true)) {
                            if (currentTitle.isNotBlank() && currentTitle.length > 5) {
                                val article = processArticle(currentTitle, currentLink, currentDesc, currentPubDate, sourceName)
                                articles.add(article)
                            }
                            insideItem = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w(TAG, "XML Parser exception for $sourceName: ${e.message}")
        }
        return articles
    }

    private fun cleanXmlText(raw: String?): String {
        if (raw == null) return ""
        return raw
            .replace(Regex("<!\\[CDATA\\[(.*?)\\]\\]>", RegexOption.DOT_MATCHES_ALL), "$1")
            .replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun processArticle(
        title: String,
        link: String,
        desc: String,
        pubDate: String,
        sourceName: String
    ): OptionBuyerNewsArticle {
        val text = "$title $desc".lowercase(Locale.ROOT)

        // Categorization & Affected Underlyings (Strictly Indian Option Markets)
        val affectedMarket: String
        val category: String

        when {
            text.contains("banknifty") || text.contains("nifty bank") || text.contains("hdfc bank") ||
                    text.contains("icici bank") || text.contains("sbi ") || text.contains("banking") -> {
                affectedMarket = "BANKNIFTY"
                category = "BANKNIFTY"
            }
            text.contains("finnifty") || text.contains("financial services") || text.contains("bajaj finance") -> {
                affectedMarket = "FINNIFTY"
                category = "FINNIFTY"
            }
            text.contains("sensex") || text.contains("bse") -> {
                affectedMarket = "SENSEX"
                category = "SENSEX"
            }
            text.contains("crude") || text.contains("crudeoil") || text.contains("brent") || text.contains("opec") -> {
                affectedMarket = "CRUDEOIL"
                category = "CRUDEOIL"
            }
            text.contains("fed") || text.contains("wall street") || text.contains("nasdaq") ||
                    text.contains("dow jones") || text.contains("global market") || text.contains("asian market") -> {
                affectedMarket = "GLOBAL"
                category = "GLOBAL"
            }
            text.contains("rbi") || text.contains("repo rate") || text.contains("inflation") ||
                    text.contains("gdp") || text.contains("budget") -> {
                affectedMarket = "NIFTY 50"
                category = "RBI / INDIA"
            }
            text.contains("fii") || text.contains("dii") || text.contains("inflow") || text.contains("outflow") -> {
                affectedMarket = "NIFTY 50"
                category = "FII / DII"
            }
            text.contains("vix") || text.contains("volatility") -> {
                affectedMarket = "NIFTY 50"
                category = "VOLATILITY"
            }
            text.contains("nifty") || text.contains("dalal street") || text.contains("stock market") -> {
                affectedMarket = "NIFTY 50"
                category = "NIFTY 50"
            }
            else -> {
                affectedMarket = "NIFTY 50"
                category = "STOCK NEWS"
            }
        }

        // Sentiment & Option Buyer Impact
        val bullishKeywords = listOf("surge", "jump", "rally", "gain", "high", "rise", "soar", "bull", "upbeat", "positive", "profit", "expansion", "rate cut", "inflow", "record high", "green")
        val bearishKeywords = listOf("fall", "drop", "slump", "plunge", "decline", "crash", "loss", "bear", "down", "negative", "tariff", "war", "escalat", "selloff", "outflow", "red")
        val highImpactKeywords = listOf("rbi", "fed", "rate hike", "rate cut", "gdp", "budget", "war", "crude spike", "earnings", "quarterly result", "crisis", "emergency", "breakout")

        var bullishCount = 0
        var bearishCount = 0
        bullishKeywords.forEach { if (text.contains(it)) bullishCount++ }
        bearishKeywords.forEach { if (text.contains(it)) bearishCount++ }

        val impact: String
        val optionBuyerBias: String

        if (bullishCount > bearishCount) {
            impact = "BULLISH"
            optionBuyerBias = "CE WATCH"
        } else if (bearishCount > bullishCount) {
            impact = "BEARISH"
            optionBuyerBias = "PE WATCH"
        } else {
            impact = "NEUTRAL"
            optionBuyerBias = "WAIT"
        }

        val isHighImpact = highImpactKeywords.any { text.contains(it) }
        val impactStrength = if (isHighImpact) "HIGH" else if (bullishCount + bearishCount >= 2) "MEDIUM" else "LOW"

        val breakingKeywords = listOf("breaking", "flash", "alert", "just in", "surges over", "plunges over", "emergency", "record high")
        val isBreaking = breakingKeywords.any { text.contains(it) }

        var confidence = 70 + kotlin.math.min(25, (bullishCount + bearishCount) * 5 + (if (isHighImpact) 10 else 0))
        if (confidence > 94) confidence = 94

        val impactReason = when (impact) {
            "BULLISH" -> "Positive momentum catalyst in $affectedMarket. Option buyers may monitor for CE entry setup on 15m candle breakout above resistance."
            "BEARISH" -> "Overhead supply pressure in $affectedMarket. Option buyers may monitor for PE entry setup if key support fails."
            else -> "Balanced sentiment. Wait for opening range resolution and Option Chain OI accumulation before entering."
        }

        var formattedTime = "Today"
        if (pubDate.isNotBlank()) {
            try {
                val inputFormats = listOf(
                    SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
                    SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
                )
                var parsedDate: Date? = null
                for (fmt in inputFormats) {
                    try {
                        parsedDate = fmt.parse(pubDate)
                        if (parsedDate != null) break
                    } catch (_: Exception) {}
                }
                if (parsedDate != null) {
                    val outFmt = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
                    }
                    formattedTime = "${outFmt.format(parsedDate)} IST"
                }
            } catch (_: Exception) {
                formattedTime = "Today"
            }
        }

        val id = "news_" + title.hashCode().toString().replace("-", "n")

        return OptionBuyerNewsArticle(
            id = id,
            headline = title,
            source = sourceName,
            publishedTime = formattedTime,
            summary = if (desc.length > 260) desc.take(257) + "..." else if (desc.isNotBlank()) desc else title,
            category = category,
            isBreaking = isBreaking,
            affectedMarket = affectedMarket,
            impact = impact,
            impactStrength = impactStrength,
            optionBuyerBias = optionBuyerBias,
            confidencePercent = confidence,
            impactReason = impactReason,
            url = link.ifBlank { null }
        )
    }
}
