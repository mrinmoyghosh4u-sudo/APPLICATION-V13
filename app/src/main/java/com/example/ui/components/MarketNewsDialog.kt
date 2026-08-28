package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.data.network.MarketIntelligenceService
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketNewsDialog(
    viewModel: MainViewModel? = null,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val marketDataMap by MarketDataStore.marketData.collectAsStateWithLifecycle()
    val intelligenceState by MarketIntelligenceService.intelligenceState.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedAiIndex by remember { mutableStateOf("NIFTY 50") }

    BackHandler(enabled = true) {
        onDismiss()
    }

    LaunchedEffect(Unit) {
        MarketIntelligenceService.refreshIntelligence(marketDataMap)
        viewModel?.notifyBreakingNews(MarketIntelligenceService.intelligenceState.value.breakingNews)
    }

    // Full Screen Premium Modal Dialog
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("market_news_screen"),
            color = Color(0xFF090A0C)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // ==========================================
                // 1. TOP HEADER & SESSION BAR
                // ==========================================
                Surface(
                    color = Color(0xFF101217),
                    border = BorderStroke(0.6.dp, Color(0xFF20242D))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF191D24), CircleShape)
                                        .border(0.6.dp, Color(0xFF2A303C), CircleShape)
                                        .testTag("news_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = PrimaryGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "MARKET INTELLIGENCE",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = PrimaryGold.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(0.5.dp, PrimaryGold.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = "OPTION BUYER",
                                                color = PrimaryGold,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Pre-Market • Global Cues • Option Buyer Impact",
                                        color = TextGray,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            MarketIntelligenceService.refreshIntelligence(marketDataMap, forceReload = true)
                                            Toast.makeText(context, "Market Intelligence Refreshed", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF191D24), CircleShape)
                                        .border(0.6.dp, Color(0xFF2A303C), CircleShape)
                                        .testTag("news_refresh_button")
                                ) {
                                    if (intelligenceState.isRefreshing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = PrimaryGold
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh",
                                            tint = PrimaryGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Market Session Status Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF151820), RoundedCornerShape(6.dp))
                                .border(0.5.dp, Color(0xFF252A36), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            when (intelligenceState.session) {
                                                MarketSessionType.MARKET_OPEN -> ProfitGreen
                                                MarketSessionType.PRE_MARKET -> PrimaryGold
                                                MarketSessionType.MARKET_CLOSED -> Color(0xFF8E95A5)
                                            },
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = intelligenceState.sessionLabel,
                                    color = when (intelligenceState.session) {
                                        MarketSessionType.MARKET_OPEN -> ProfitGreen
                                        MarketSessionType.PRE_MARKET -> PrimaryGold
                                        MarketSessionType.MARKET_CLOSED -> TextWhite
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = intelligenceState.istTime.ifBlank { "09:15:00 AM IST" },
                                color = TextGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ==========================================
                // 2. CATEGORY FILTER TABS
                // ==========================================
                val categories = listOf(
                    "ALL", "PRE-MARKET", "BREAKING", "HIGH IMPACT",
                    "NIFTY 50", "BANKNIFTY", "FINNIFTY", "SENSEX",
                    "GLOBAL", "RBI / INDIA", "CRUDEOIL", "FII / DII", "VOLATILITY", "STOCK NEWS"
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C0E12))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedCategory = cat }
                                .testTag("news_cat_$cat"),
                            color = if (isSelected) PrimaryGold else Color(0xFF171A21),
                            border = BorderStroke(
                                0.6.dp,
                                if (isSelected) PrimaryGold else Color(0xFF262C38)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.Black else TextGray,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // ==========================================
                // 3. MAIN CONTENT BODY
                // ==========================================
                if (intelligenceState.isLoading && intelligenceState.giftNifty == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryGold, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Aggregating Market Intelligence...", color = Color.White, fontSize = 13.sp)
                            Text("Syncing GIFT NIFTY, India VIX & Option Buyer Cues", color = TextGray, fontSize = 11.sp)
                        }
                    }
                } else {
                    val filteredNews = remember(selectedCategory, intelligenceState.newsArticles) {
                        when (selectedCategory) {
                            "ALL" -> intelligenceState.newsArticles
                            "PRE-MARKET" -> emptyList() // handled in Pre-Market section
                            "BREAKING" -> intelligenceState.newsArticles.filter { it.isBreaking }
                            "HIGH IMPACT" -> intelligenceState.newsArticles.filter { it.impactStrength == "HIGH" }
                            else -> intelligenceState.newsArticles.filter {
                                it.category.equals(selectedCategory, ignoreCase = true) ||
                                        it.affectedMarket.contains(selectedCategory, ignoreCase = true)
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Section A: PRE-MARKET ANALYSIS (Shown on ALL or PRE-MARKET tab)
                        if (selectedCategory == "ALL" || selectedCategory == "PRE-MARKET") {
                            item {
                                PreMarketAnalysisSection(
                                    giftNifty = intelligenceState.giftNifty,
                                    indiaVix = intelligenceState.indiaVix,
                                    globalCues = intelligenceState.globalCues,
                                    fiiDii = intelligenceState.fiiDii,
                                    preMarketLevels = intelligenceState.preMarketLevels
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                AiOptionBuyerViewSection(
                                    selectedSymbol = selectedAiIndex,
                                    onSelectSymbol = { selectedAiIndex = it },
                                    signals = intelligenceState.aiOptionBuyerSignals
                                )
                            }
                        }

                        // Section B: BREAKING NEWS CARDS (Shown on ALL, BREAKING, or specific category)
                        if (selectedCategory != "PRE-MARKET" && intelligenceState.breakingNews.isNotEmpty() && (selectedCategory == "ALL" || selectedCategory == "BREAKING")) {
                            item {
                                SectionHeader(
                                    title = "🔥 BREAKING NEWS & IMPACT",
                                    subtitle = "Real-time High-Impact Market Catalysts"
                                )
                            }

                            items(intelligenceState.breakingNews, key = { "breaking_${it.id}" }) { article ->
                                BreakingNewsCard(article = article)
                            }
                        }

                        // Section C: OPTION BUYER NEWS FEED
                        if (selectedCategory != "PRE-MARKET") {
                            item {
                                SectionHeader(
                                    title = if (selectedCategory == "ALL") "📰 LATEST MARKET NEWS & OPTION IMPACT" else "📰 $selectedCategory NEWS",
                                    subtitle = "Option Buyer Sentiment • CE / PE Watchlist Bias"
                                )
                            }

                            if (filteredNews.isEmpty()) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFF13161C),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(0.6.dp, Color(0xFF242832))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Info,
                                                contentDescription = null,
                                                tint = TextGray,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "No Articles Found in $selectedCategory",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Switch category to ALL or check back for new announcements.",
                                                color = TextGray,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(filteredNews, key = { it.id }) { article ->
                                    MarketNewsArticleCard(article = article)
                                }
                            }
                        }

                        // Section D: DISCLAIMER FOOTER
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 20.dp),
                                color = Color(0xFF101217),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, Color(0xFF1F242E))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = PrimaryGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "OPTION BUYER RISK & EXECUTION RULES",
                                            color = PrimaryGold,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "News & Pre-Market analysis provide strategic bias only. News alone NEVER triggers automated orders. Live trade execution requires real-time Option Chain OI, Volume, PCR, and technical chart confirmation. Maintain strict stoplosses.",
                                        color = TextGray,
                                        fontSize = 9.5.sp,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SUB-COMPONENTS: PRE-MARKET ANALYSIS
// =========================================================================

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )
        Text(
            text = subtitle,
            color = TextGray,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun PreMarketAnalysisSection(
    giftNifty: GiftNiftyData?,
    indiaVix: IndiaVixData?,
    globalCues: List<GlobalCueItem>,
    fiiDii: FiiDiiFlowData?,
    preMarketLevels: List<PreMarketIndexLevels>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "🌅 PRE-MARKET ANALYSIS",
            subtitle = "GIFT NIFTY • India VIX • Global Cues • S/R Pivot Levels"
        )

        // 1. GIFT NIFTY CARD
        if (giftNifty != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF13171F),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.8.dp, Color(0xFF262E3D))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF1D2330), CircleShape)
                                    .border(0.6.dp, Color(0xFF333D50), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = PrimaryGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "GIFT NIFTY",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = giftNifty.source,
                                    color = TextGray,
                                    fontSize = 9.5.sp
                                )
                            }
                        }

                        // Gap Badge
                        Surface(
                            color = when (giftNifty.gapStatus) {
                                "GAP UP" -> ProfitGreen.copy(alpha = 0.18f)
                                "GAP DOWN" -> LossRed.copy(alpha = 0.18f)
                                else -> Color(0xFF262B35)
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                0.6.dp,
                                when (giftNifty.gapStatus) {
                                    "GAP UP" -> ProfitGreen
                                    "GAP DOWN" -> LossRed
                                    else -> Color(0xFF3F4655)
                                }
                            )
                        ) {
                            Text(
                                text = "${giftNifty.gapStatus} (${if (giftNifty.gapPoints >= 0) "+" else ""}${String.format("%.1f", giftNifty.gapPoints)} pts)",
                                color = when (giftNifty.gapStatus) {
                                    "GAP UP" -> ProfitGreen
                                    "GAP DOWN" -> LossRed
                                    else -> TextWhite
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Key Values 3-column row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PreMarketMetric(
                            label = "GIFT NIFTY LTP",
                            value = "₹${String.format("%,.2f", giftNifty.ltp)}",
                            subValue = "${if (giftNifty.change >= 0) "+" else ""}${String.format("%.2f", giftNifty.changePercent)}%",
                            isPositive = giftNifty.change >= 0
                        )

                        PreMarketMetric(
                            label = "PREV NIFTY CLOSE",
                            value = "₹${String.format("%,.2f", giftNifty.prevCloseNifty)}",
                            subValue = "NSE Official Close",
                            isPositive = null
                        )

                        PreMarketMetric(
                            label = "IMPLIED OPEN",
                            value = "₹${String.format("%,.2f", giftNifty.impliedNiftyOpen)}",
                            subValue = "Indicative Only",
                            isPositive = giftNifty.gapPoints >= 0
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Notice
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF181C26), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = SecondaryGold,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = giftNifty.disclaimer,
                                color = TextGray,
                                fontSize = 9.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. INDIA VIX CARD
        if (indiaVix != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF13171F),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.8.dp, Color(0xFF262E3D))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF1D2330), CircleShape)
                                    .border(0.6.dp, Color(0xFF333D50), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = PrimaryGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "INDIA VIX (VOLATILITY)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Option Pricing & Theta Context",
                                    color = TextGray,
                                    fontSize = 9.5.sp
                                )
                            }
                        }

                        // Volatility Status Badge
                        Surface(
                            color = when (indiaVix.status) {
                                "LOW" -> Color(0xFF162E20)
                                "NORMAL" -> PrimaryGold.copy(alpha = 0.2f)
                                "HIGH" -> Color(0xFF3E2214)
                                else -> LossRed.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                0.6.dp,
                                when (indiaVix.status) {
                                    "LOW" -> ProfitGreen
                                    "NORMAL" -> PrimaryGold
                                    "HIGH" -> SecondaryGold
                                    else -> LossRed
                                }
                            )
                        ) {
                            Text(
                                text = "${indiaVix.status} VOLATILITY",
                                color = when (indiaVix.status) {
                                    "LOW" -> ProfitGreen
                                    "NORMAL" -> PrimaryGold
                                    "HIGH" -> SecondaryGold
                                    else -> LossRed
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format("%.2f", indiaVix.ltp),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${if (indiaVix.change >= 0) "+" else ""}${String.format("%.2f", indiaVix.change)} (${if (indiaVix.changePercent >= 0) "+" else ""}${String.format("%.2f", indiaVix.changePercent)}%)",
                                color = if (indiaVix.change <= 0) ProfitGreen else LossRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option Buyer Context Advice
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF181C26), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = PrimaryGold,
                                modifier = Modifier.size(14.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = indiaVix.optionBuyerAdvice,
                                color = TextWhite,
                                fontSize = 9.5.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. GLOBAL MARKET CUES HORIZONTAL GRID
        if (globalCues.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "GLOBAL MARKET CUES",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(globalCues) { cue ->
                        Surface(
                            modifier = Modifier.width(135.dp),
                            color = Color(0xFF13171F),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.6.dp, Color(0xFF232A36))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cue.symbol,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = cue.region,
                                        color = TextGray,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = String.format("%,.2f", cue.ltp),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (cue.change >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (cue.change >= 0) ProfitGreen else LossRed,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${if (cue.change >= 0) "+" else ""}${String.format("%.2f", cue.changePercent)}%",
                                        color = if (cue.change >= 0) ProfitGreen else LossRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. FII / DII INSTITUTIONAL CASH FLOW
        if (fiiDii != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF13171F),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.8.dp, Color(0xFF262E3D))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FII & DII INSTITUTIONAL FLOW",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = fiiDii.dateFormatted,
                                color = TextGray,
                                fontSize = 9.sp
                            )
                        }

                        Surface(
                            color = ProfitGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, ProfitGreen.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = fiiDii.institutionalBias,
                                color = ProfitGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PreMarketMetric(
                            label = "FII NET (CASH)",
                            value = "${if (fiiDii.fiiNet >= 0) "+₹" else "-₹"}${String.format("%,.1f", fiiDii.fiiNet)} Cr",
                            subValue = "Buy: ₹${fiiDii.fiiBuy.toInt()}Cr",
                            isPositive = fiiDii.fiiNet >= 0
                        )

                        PreMarketMetric(
                            label = "DII NET (CASH)",
                            value = "${if (fiiDii.diiNet >= 0) "+₹" else "-₹"}${String.format("%,.1f", fiiDii.diiNet)} Cr",
                            subValue = "Buy: ₹${fiiDii.diiBuy.toInt()}Cr",
                            isPositive = fiiDii.diiNet >= 0
                        )

                        PreMarketMetric(
                            label = "NET TOTAL FLOW",
                            value = "${if (fiiDii.totalNet >= 0) "+₹" else "-₹"}${String.format("%,.1f", fiiDii.totalNet)} Cr",
                            subValue = "Combined Cash",
                            isPositive = fiiDii.totalNet >= 0
                        )
                    }
                }
            }
        }

        // 5. PRE-MARKET S/R & PIVOT LEVELS
        if (preMarketLevels.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "PRE-MARKET SUPPORT & RESISTANCE LEVELS",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                preMarketLevels.take(2).forEach { level ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        color = Color(0xFF13171F),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.6.dp, Color(0xFF232A36))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = level.symbol,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (level.bias == "BULLISH") ProfitGreen.copy(alpha = 0.15f) else LossRed.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = level.bias,
                                            color = if (level.bias == "BULLISH") ProfitGreen else LossRed,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Implied: ₹${String.format("%,.0f", level.giftNiftyImpliedOpen)}",
                                    color = PrimaryGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 4-box S/R Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LevelBox(label = "S2", price = level.support2, color = LossRed, modifier = Modifier.weight(1f))
                                LevelBox(label = "S1", price = level.support1, color = LossRed, modifier = Modifier.weight(1f))
                                LevelBox(label = "PIVOT", price = level.pivot, color = PrimaryGold, modifier = Modifier.weight(1f))
                                LevelBox(label = "R1", price = level.resistance1, color = ProfitGreen, modifier = Modifier.weight(1f))
                                LevelBox(label = "R2", price = level.resistance2, color = ProfitGreen, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelBox(label: String, price: Double, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF181C26), RoundedCornerShape(6.dp))
            .border(0.5.dp, Color(0xFF262C3A), RoundedCornerShape(6.dp))
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = color, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            Text(text = "₹${price.toInt()}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun PreMarketMetric(
    label: String,
    value: String,
    subValue: String,
    isPositive: Boolean?
) {
    Column {
        Text(text = label, color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = subValue,
            color = when (isPositive) {
                true -> ProfitGreen
                false -> LossRed
                null -> TextGray
            },
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// =========================================================================
// SUB-COMPONENTS: AI PRE-MARKET OPTION BUYER VIEW
// =========================================================================

@Composable
private fun AiOptionBuyerViewSection(
    selectedSymbol: String,
    onSelectSymbol: (String) -> Unit,
    signals: List<AiPreMarketOptionBuyerSignal>
) {
    val symbolList = listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "SENSEX", "CRUDEOIL")
    val activeSignal = signals.find { it.symbol.equals(selectedSymbol, ignoreCase = true) }
        ?: signals.firstOrNull()
        ?: AiPreMarketOptionBuyerSignal(
            symbol = selectedSymbol,
            preMarketBias = "BULLISH",
            optionBuyerBias = "CE WATCH",
            confidence = 78,
            reason = "Positive global cues + GIFT NIFTY gap up + strong institutional cash accumulation."
        )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF131720),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(PrimaryGold.copy(alpha = 0.2f), CircleShape)
                            .border(0.8.dp, PrimaryGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = PrimaryGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "🤖 AI PRE-MARKET OPTION BUYER VIEW",
                            color = PrimaryGold,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Strategic watchlist setup before market open",
                            color = TextGray,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Index Selector Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(symbolList) { sym ->
                    val isSelected = sym == selectedSymbol
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectSymbol(sym) },
                        color = if (isSelected) PrimaryGold else Color(0xFF1B202A),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, if (isSelected) PrimaryGold else Color(0xFF2C3444))
                    ) {
                        Text(
                            text = sym,
                            color = if (isSelected) Color.Black else TextWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Signal Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF191E28), RoundedCornerShape(8.dp))
                    .border(0.6.dp, Color(0xFF293244), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "PRE-MARKET BIAS", color = TextGray, fontSize = 9.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (activeSignal.preMarketBias == "BULLISH") ProfitGreen else if (activeSignal.preMarketBias == "BEARISH") LossRed else TextGray,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = activeSignal.preMarketBias,
                                    color = if (activeSignal.preMarketBias == "BULLISH") ProfitGreen else if (activeSignal.preMarketBias == "BEARISH") LossRed else TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "OPTION BUYER BIAS", color = TextGray, fontSize = 9.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                color = when (activeSignal.optionBuyerBias) {
                                    "CE WATCH" -> ProfitGreen.copy(alpha = 0.2f)
                                    "PE WATCH" -> LossRed.copy(alpha = 0.2f)
                                    else -> Color(0xFF2C3240)
                                },
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(
                                    0.6.dp,
                                    when (activeSignal.optionBuyerBias) {
                                        "CE WATCH" -> ProfitGreen
                                        "PE WATCH" -> LossRed
                                        else -> TextGray
                                    }
                                )
                            ) {
                                Text(
                                    text = activeSignal.optionBuyerBias,
                                    color = when (activeSignal.optionBuyerBias) {
                                        "CE WATCH" -> ProfitGreen
                                        "PE WATCH" -> LossRed
                                        else -> TextWhite
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "AI CONFIDENCE", color = TextGray, fontSize = 9.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${activeSignal.confidence}%",
                                color = PrimaryGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Synthesized Analysis:",
                        color = SecondaryGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activeSignal.reason,
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Warning / Execution Rule
                    Text(
                        text = "⚠️ ${activeSignal.disclaimer}",
                        color = TextGray,
                        fontSize = 8.5.sp,
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
}

// =========================================================================
// SUB-COMPONENTS: BREAKING NEWS & LATEST NEWS CARDS
// =========================================================================

@Composable
private fun BreakingNewsCard(article: OptionBuyerNewsArticle) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF181510),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFB87333))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = LossRed.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.6.dp, LossRed)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(LossRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "BREAKING",
                            color = LossRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "${article.source} • ${article.publishedTime}",
                    color = TextGray,
                    fontSize = 9.5.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = article.headline,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = article.summary,
                color = Color(0xFFCBCED6),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Option Buyer Impact Strip
            OptionBuyerImpactBadge(article = article)
        }
    }
}

@Composable
private fun MarketNewsArticleCard(article: OptionBuyerNewsArticle) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF13161C),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.6.dp, Color(0xFF232732))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF1E232E),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = article.category,
                            color = PrimaryGold,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = article.source,
                        color = TextGray,
                        fontSize = 9.sp
                    )
                }

                Text(
                    text = article.publishedTime,
                    color = TextGray,
                    fontSize = 9.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = article.headline,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = article.summary,
                color = TextGray,
                fontSize = 10.5.sp,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            OptionBuyerImpactBadge(article = article)
        }
    }
}

@Composable
private fun OptionBuyerImpactBadge(article: OptionBuyerNewsArticle) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF181C26), RoundedCornerShape(6.dp))
            .border(0.5.dp, Color(0xFF262C3A), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Target: ${article.affectedMarket}",
                        color = PrimaryGold,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• Impact: ${article.impact}",
                        color = if (article.impact == "BULLISH") ProfitGreen else if (article.impact == "BEARISH") LossRed else TextWhite,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = when (article.optionBuyerBias) {
                        "CE WATCH" -> ProfitGreen.copy(alpha = 0.2f)
                        "PE WATCH" -> LossRed.copy(alpha = 0.2f)
                        else -> Color(0xFF2C3240)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${article.optionBuyerBias} (${article.confidencePercent}%)",
                        color = when (article.optionBuyerBias) {
                            "CE WATCH" -> ProfitGreen
                            "PE WATCH" -> LossRed
                            else -> TextWhite
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "⚡ ${article.impactReason}",
                color = TextWhite,
                fontSize = 9.sp,
                lineHeight = 12.5.sp
            )
        }
    }
}
