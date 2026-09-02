const https = require('https');
const http = require('http');

/**
 * Public Market News & Intelligence Aggregator for KING KHAN AI TRADER
 * Aggregates public RSS feeds from leading Indian financial news publications:
 * - The Economic Times (Markets & Stocks)
 * - Moneycontrol (Top News & Market Reports)
 * - Livemint (Markets)
 * - Financial Express (Markets)
 */

const RSS_FEEDS = [
  {
    name: 'Economic Times',
    url: 'https://economictimes.indiatimes.com/markets/rssfeeds/1977021501.cms',
    defaultCategory: 'MARKETS'
  },
  {
    name: 'Economic Times Stocks',
    url: 'https://economictimes.indiatimes.com/markets/stocks/rssfeeds/2146842.cms',
    defaultCategory: 'STOCK NEWS'
  },
  {
    name: 'Moneycontrol',
    url: 'https://www.moneycontrol.com/rss/marketreports.xml',
    defaultCategory: 'MARKETS'
  },
  {
    name: 'Moneycontrol Top News',
    url: 'https://www.moneycontrol.com/rss/MCtopnews.xml',
    defaultCategory: 'BREAKING'
  },
  {
    name: 'Livemint Markets',
    url: 'https://www.livemint.com/rss/markets',
    defaultCategory: 'MARKETS'
  }
];

function fetchFeed(feedObj) {
  return new Promise((resolve) => {
    try {
      const parsedUrl = new URL(feedObj.url);
      const client = parsedUrl.protocol === 'https:' ? https : http;
      const options = {
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
          'Accept': 'application/rss+xml, application/xml, text/xml, */*'
        },
        timeout: 6000
      };

      const req = client.get(feedObj.url, options, (res) => {
        if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          // Follow redirect once
          client.get(res.headers.location, options, (redirRes) => {
            let data = '';
            redirRes.on('data', (chunk) => { data += chunk; });
            redirRes.on('end', () => resolve({ name: feedObj.name, xml: data }));
          }).on('error', () => resolve({ name: feedObj.name, xml: '' }));
          return;
        }

        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => resolve({ name: feedObj.name, xml: data }));
      });

      req.on('error', () => resolve({ name: feedObj.name, xml: '' }));
      req.on('timeout', () => {
        req.destroy();
        resolve({ name: feedObj.name, xml: '' });
      });
    } catch (e) {
      resolve({ name: feedObj.name, xml: '' });
    }
  });
}

function cleanHtml(raw) {
  if (!raw) return '';
  return raw
    .replace(/<!\[CDATA\[(.*?)\]\]>/gs, '$1')
    .replace(/<[^>]*>/g, '')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&nbsp;/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function parseRssXml(xml, sourceName) {
  const articles = [];
  if (!xml || xml.length < 50) return articles;

  const itemRegex = /<item\b[^>]*>([\s\S]*?)<\/item>/gi;
  let match;

  while ((match = itemRegex.exec(xml)) !== null) {
    const itemContent = match[1];

    const titleMatch = /<title\b[^>]*>([\s\S]*?)<\/title>/i.exec(itemContent);
    const linkMatch = /<link\b[^>]*>([\s\S]*?)<\/link>/i.exec(itemContent) || /<guid\b[^>]*>([\s\S]*?)<\/guid>/i.exec(itemContent);
    const descMatch = /<description\b[^>]*>([\s\S]*?)<\/description>/i.exec(itemContent);
    const pubDateMatch = /<pubDate\b[^>]*>([\s\S]*?)<\/pubDate>/i.exec(itemContent);

    const rawTitle = titleMatch ? cleanHtml(titleMatch[1]) : '';
    const rawLink = linkMatch ? cleanHtml(linkMatch[1]) : '';
    const rawDesc = descMatch ? cleanHtml(descMatch[1]) : '';
    const rawDate = pubDateMatch ? cleanHtml(pubDateMatch[1]) : '';

    if (!rawTitle || rawTitle.length < 5) continue;

    // Categorization & Option Buyer Impact Analysis
    const textToAnalyze = (rawTitle + ' ' + rawDesc).toLowerCase();

    let affectedMarket = 'NIFTY 50';
    let category = 'ALL';

    if (textToAnalyze.includes('banknifty') || textToAnalyze.includes('nifty bank') || textToAnalyze.includes('hdfc bank') || textToAnalyze.includes('icici bank') || textToAnalyze.includes('sbi ') || textToAnalyze.includes('banking')) {
      affectedMarket = 'BANKNIFTY';
      category = 'BANKNIFTY';
    } else if (textToAnalyze.includes('finnifty') || textToAnalyze.includes('financial services') || textToAnalyze.includes('bajaj finance')) {
      affectedMarket = 'FINNIFTY';
      category = 'FINNIFTY';
    } else if (textToAnalyze.includes('sensex') || textToAnalyze.includes('bse')) {
      affectedMarket = 'SENSEX';
      category = 'SENSEX';
    } else if (textToAnalyze.includes('crude') || textToAnalyze.includes('oil') || textToAnalyze.includes('opec') || textToAnalyze.includes('brent')) {
      affectedMarket = 'CRUDEOIL';
      category = 'CRUDEOIL';
    } else if (textToAnalyze.includes('fed') || textToAnalyze.includes('wall street') || textToAnalyze.includes('nasdaq') || textToAnalyze.includes('dow jones') || textToAnalyze.includes('global market') || textToAnalyze.includes('asian market') || textToAnalyze.includes('tariffs')) {
      affectedMarket = 'GLOBAL';
      category = 'GLOBAL';
    } else if (textToAnalyze.includes('rbi') || textToAnalyze.includes('repo rate') || textToAnalyze.includes('inflation') || textToAnalyze.includes('gdp') || textToAnalyze.includes('budget') || textToAnalyze.includes('fiscal')) {
      affectedMarket = 'NIFTY 50';
      category = 'RBI / INDIA';
    } else if (textToAnalyze.includes('fii') || textToAnalyze.includes('dii') || textToAnalyze.includes('inflow') || textToAnalyze.includes('outflow') || textToAnalyze.includes('institutional')) {
      affectedMarket = 'NIFTY 50';
      category = 'FII / DII';
    } else if (textToAnalyze.includes('vix') || textToAnalyze.includes('volatility')) {
      affectedMarket = 'NIFTY 50';
      category = 'VOLATILITY';
    } else if (textToAnalyze.includes('nifty') || textToAnalyze.includes('dalal street') || textToAnalyze.includes('shares') || textToAnalyze.includes('stock market')) {
      affectedMarket = 'NIFTY 50';
      category = 'NIFTY 50';
    } else {
      category = 'STOCK NEWS';
    }

    // Impact Analysis (Bullish / Bearish / Neutral)
    const bullishWords = ['surge', 'jump', 'rally', 'gain', 'high', 'rise', 'soar', 'bull', 'upbeat', 'positive', 'profit', 'expansion', 'rate cut', 'inflow', 'record high', 'green'];
    const bearishWords = ['fall', 'drop', 'slump', 'plunge', 'decline', 'crash', 'loss', 'bear', 'down', 'negative', 'tariff', 'war', 'escalat', 'selloff', 'outflow', 'red', 'inflation high'];
    const highImpactWords = ['rbi', 'fed', 'rate hike', 'rate cut', 'gdp', 'budget', 'war', 'crude spike', 'earnings', 'quarterly result', 'crisis', 'emergency', 'breakout'];

    let bullishCount = 0;
    let bearishCount = 0;

    bullishWords.forEach(w => { if (textToAnalyze.includes(w)) bullishCount++; });
    bearishWords.forEach(w => { if (textToAnalyze.includes(w)) bearishCount++; });

    let impact = 'NEUTRAL';
    let optionBuyerBias = 'WAIT';
    if (bullishCount > bearishCount) {
      impact = 'BULLISH';
      optionBuyerBias = 'CE WATCH';
    } else if (bearishCount > bullishCount) {
      impact = 'BEARISH';
      optionBuyerBias = 'PE WATCH';
    }

    let isHighImpact = highImpactWords.some(w => textToAnalyze.includes(w));
    let impactStrength = isHighImpact ? 'HIGH' : (bullishCount + bearishCount >= 2 ? 'MEDIUM' : 'LOW');

    const breakingKeywords = ['breaking', 'flash', 'alert', 'just in', 'surges over', 'plunges over', 'records massive', 'emergency'];
    let isBreaking = breakingKeywords.some(w => textToAnalyze.includes(w));

    let confidence = 70 + Math.min(25, (bullishCount + bearishCount) * 5 + (isHighImpact ? 10 : 0));
    if (confidence > 94) confidence = 94;

    let impactReason = '';
    if (impact === 'BULLISH') {
      impactReason = `Positive catalyst in ${affectedMarket}. Option buyers may monitor for CE entry setup on 15m candle breakout above resistance.`;
    } else if (impact === 'BEARISH') {
      impactReason = `Overhead supply pressure in ${affectedMarket}. Option buyers may monitor for PE entry setup if key support fails.`;
    } else {
      impactReason = `Balanced sentiment. Wait for opening range resolution and Option Chain OI accumulation before entering.`;
    }

    // Format display time
    let displayTime = 'Just now';
    if (rawDate) {
      try {
        const d = new Date(rawDate);
        if (!isNaN(d.getTime())) {
          displayTime = d.toLocaleTimeString('en-US', { timeZone: 'Asia/Kolkata', hour: '2-digit', minute: '2-digit', hour12: true }) + ' IST';
        }
      } catch (e) {
        displayTime = 'Today';
      }
    }

    // Hash ID
    const id = Buffer.from(rawTitle).toString('base64').substring(0, 24).replace(/[^a-zA-Z0-9]/g, '');

    articles.push({
      id: id || `news_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
      headline: rawTitle,
      source: sourceName,
      publishedTime: displayTime,
      summary: rawDesc ? rawDesc.substring(0, 260) + (rawDesc.length > 260 ? '...' : '') : rawTitle,
      category: category,
      isBreaking: isBreaking,
      affectedMarket: affectedMarket,
      impact: impact,
      impactStrength: impactStrength,
      optionBuyerBias: optionBuyerBias,
      confidencePercent: confidence,
      impactReason: impactReason,
      url: rawLink || null
    });
  }

  return articles;
}

module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  res.setHeader('Cache-Control', 's-maxage=120, stale-while-revalidate=300');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  try {
    const feedPromises = RSS_FEEDS.map(f => fetchFeed(f));
    const results = await Promise.all(feedPromises);

    let allArticles = [];
    results.forEach(r => {
      const parsed = parseRssXml(r.xml, r.name);
      allArticles = allArticles.concat(parsed);
    });

    // Deduplicate by title similarity or ID
    const seenTitles = new Set();
    const uniqueArticles = [];

    for (const article of allArticles) {
      const normTitle = article.headline.toLowerCase().replace(/[^a-z0-9]/g, '').substring(0, 35);
      if (!seenTitles.has(normTitle)) {
        seenTitles.add(normTitle);
        uniqueArticles.push(article);
      }
    }

    return res.status(200).json({
      status: 'success',
      count: uniqueArticles.length,
      lastUpdated: new Date().toISOString(),
      source: 'Verified Aggregated Financial Feeds (ET, Moneycontrol, Livemint)',
      articles: uniqueArticles
    });
  } catch (err) {
    return res.status(500).json({
      status: 'error',
      message: err.message || 'Failed to aggregate market news',
      articles: []
    });
  }
};
