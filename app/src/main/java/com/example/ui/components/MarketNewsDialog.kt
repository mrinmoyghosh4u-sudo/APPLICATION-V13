package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class MarketNewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val category: String, // "MACRO", "MCX", "STOCKS", "GLOBAL"
    val source: String,
    val timeAgo: String,
    val sentiment: String, // "BULLISH", "BEARISH", "NEUTRAL"
    val impact: String = "HIGH"
)

@Composable
fun MarketNewsDialog(
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var isRefreshing by remember { mutableStateOf(false) }

    val newsList = remember {
        listOf(
            MarketNewsArticle(
                id = "1",
                title = "RBI MPC Meeting Update: Repo Rate Held Steady, Liquidity Stance Maintained",
                summary = "The Reserve Bank of India kept the benchmark repo rate unchanged at 6.50% while emphasizing continued commitment to aligning inflation with the 4% target. Banking and NBFC counters see positive traction.",
                category = "MACRO",
                source = "RBI Bulletin / Exchange Wire",
                timeAgo = "12m ago",
                sentiment = "BULLISH",
                impact = "HIGH"
            ),
            MarketNewsArticle(
                id = "2",
                title = "Crude Oil Surges Above \$76 as Middle East Supply Tensions Escalate",
                summary = "WTI and Brent Crude extended intraday gains following lower-than-expected inventory build and heightened geopolitical risk premiums in key maritime transit corridors. MCX Crude contracts trading up over 1.8%.",
                category = "MCX",
                source = "MCX / Reuters Energy",
                timeAgo = "25m ago",
                sentiment = "BULLISH",
                impact = "HIGH"
            ),
            MarketNewsArticle(
                id = "3",
                title = "FII & DII Cash Activity: Domestic Funds Inject ₹2,840 Cr In Cash Market",
                summary = "DIIs remained strong net buyers absorbing intermittent foreign fund outflows. Key institutional accumulation noted in large-cap banking, metal, and industrial heavyweights.",
                category = "MACRO",
                source = "NSE Daily Institutional Feed",
                timeAgo = "42m ago",
                sentiment = "BULLISH",
                impact = "MEDIUM"
            ),
            MarketNewsArticle(
                id = "4",
                title = "Gold & Silver MCX Contracts Rally on Safe-Haven Inflows & Dollar Softness",
                summary = "MCX Gold 10g and Silver 1kg contracts tracked international benchmarks higher as US Treasury yields eased slightly ahead of upcoming core PCE inflation readings.",
                category = "MCX",
                source = "Bullion Desk / Bloomberg",
                timeAgo = "1h ago",
                sentiment = "BULLISH",
                impact = "HIGH"
            ),
            MarketNewsArticle(
                id = "5",
                title = "Nifty IT Index Recovers Key 20-DMA Amid Resilient US Tech Earnings",
                summary = "TCS, Infosys, and HCLTech led a modest rebound after strong Q3 guidance in cloud and generative AI deal pipelines across North American client bases.",
                category = "STOCKS",
                source = "Moneycontrol Wire",
                timeAgo = "1h 30m ago",
                sentiment = "BULLISH",
                impact = "MEDIUM"
            ),
            MarketNewsArticle(
                id = "6",
                title = "Copper & Industrial Metals Steady as Global Manufacturing PMIs Show Stabilization",
                summary = "MCX Copper futures held firm above ₹820/kg supported by tight warehouse inventories and robust green energy grid demand across Asian economies.",
                category = "MCX",
                source = "LME / MCX Metals",
                timeAgo = "2h ago",
                sentiment = "NEUTRAL",
                impact = "MEDIUM"
            ),
            MarketNewsArticle(
                id = "7",
                title = "India Auto Wholesale Figures Show Double-Digit Growth in SUV & EV Portfolios",
                summary = "Leading auto manufacturers recorded higher month-on-month dispatches heading into the peak fiscal quarter, supporting automotive ancillary stocks.",
                category = "STOCKS",
                source = "SIAM Industry Report",
                timeAgo = "3h ago",
                sentiment = "BULLISH",
                impact = "MEDIUM"
            ),
            MarketNewsArticle(
                id = "8",
                title = "US Federal Reserve Signals Data-Dependent Path for Upcoming FOMC Deliberations",
                summary = "Fed officials reiterated that policy decisions will hinge on incoming labor market metrics and inflation trajectories, keeping global rate cut expectations measured.",
                category = "GLOBAL",
                source = "Dow Jones / Fed Wire",
                timeAgo = "4h ago",
                sentiment = "NEUTRAL",
                impact = "HIGH"
            )
        )
    }

    val filteredNews = remember(selectedCategory, newsList) {
        if (selectedCategory == "ALL") newsList
        else newsList.filter { it.category == selectedCategory }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Newspaper,
                        contentDescription = null,
                        tint = PrimaryGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Live Market News", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                        Text("Real-Time Indian & Global Feeds", fontSize = 10.sp, color = SecondaryGold)
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "ALL" to "All News",
                        "MACRO" to "Macro & Indices",
                        "MCX" to "MCX Commodities",
                        "STOCKS" to "Stocks & Earnings",
                        "GLOBAL" to "Global Markets"
                    ).forEach { (catKey, catLabel) ->
                        val isSelected = selectedCategory == catKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) PrimaryGold else Color(0xFF161920))
                                .border(0.6.dp, if (isSelected) PrimaryGold else DarkCardBorder, RoundedCornerShape(14.dp))
                                .clickable { selectedCategory = catKey }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = catLabel,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else TextWhite
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // News Articles List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNews, key = { it.id }) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF13161C),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFF262B34))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    when (item.sentiment) {
                                                        "BULLISH" -> ProfitGreen.copy(alpha = 0.15f)
                                                        "BEARISH" -> LossRed.copy(alpha = 0.15f)
                                                        else -> PrimaryGold.copy(alpha = 0.15f)
                                                    },
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = item.sentiment,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (item.sentiment) {
                                                    "BULLISH" -> ProfitGreen
                                                    "BEARISH" -> LossRed
                                                    else -> PrimaryGold
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = item.source, fontSize = 10.sp, color = TextGray)
                                    }
                                    Text(text = item.timeAgo, fontSize = 10.sp, color = SecondaryGold)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = item.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    lineHeight = 17.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = item.summary,
                                    fontSize = 11.sp,
                                    color = TextGray,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Text("Close News Feed", fontWeight = FontWeight.Bold)
            }
        }
    )
}
