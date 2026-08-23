package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OptionStrikeItem
import com.example.data.model.UserProfileEntity
import com.example.ui.components.CrownLogo
import com.example.ui.components.PullToRefreshLayout
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import com.example.ui.screens.OptionChainTabContent

@Composable
fun OptionChainScreen(
    userProfile: UserProfileEntity = UserProfileEntity(),
    strikes: List<OptionStrikeItem>,
    selectedOptionIndex: String,
    availableExpiries: List<String> = emptyList(),
    selectedOptionExpiry: String = "",
    marketDataSource: String = "Angel One",
    marketDataLastUpdated: String = "",
    viewModel: MainViewModel,
    onSelectIndex: (String) -> Unit,
    onSelectExpiry: (String) -> Unit = {},
    onOpenNotificationCenter: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false,
    onOpenOrderDialog: (symbol: String, side: String, price: Double?, lotSize: Int?) -> Unit
) {
    val currentIndex = if (selectedOptionIndex.isEmpty()) "NIFTY 50" else selectedOptionIndex

    LaunchedEffect(selectedOptionIndex) {
        if (selectedOptionIndex.isEmpty()) {
            onSelectIndex("NIFTY 50")
        }
    }

    PullToRefreshLayout(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CrownLogo(size = 36.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "KING KHAN AI TRADE",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        com.example.ui.components.KingKhanTagline(fontSize = 11.sp)
                    }
                }
                IconButton(onClick = onOpenNotificationCenter) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Alerts", tint = SecondaryGold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // NSE
            Text("NSE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite, modifier = Modifier.padding(horizontal = 16.dp))
            HorizontalDivider(color = DarkCardBorder, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("NIFTY 50", "BANKNIFTY", "FINNIFTY", "MIDCAP NIFTY")) { index ->
                    IndexPill(index, currentIndex, onSelectIndex)
                }
            }
            
            // BSE
            Text("BSE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite, modifier = Modifier.padding(horizontal = 16.dp))
            HorizontalDivider(color = DarkCardBorder, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("SENSEX", "BANKEX")) { index ->
                    IndexPill(index, currentIndex, onSelectIndex)
                }
            }
            
            // MCX
            Text("MCX", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite, modifier = Modifier.padding(horizontal = 16.dp))
            HorizontalDivider(color = DarkCardBorder, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("CRUDEOIL", "CRUDEOIL M")) { index ->
                    IndexPill(index, currentIndex, onSelectIndex)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            val exchange = when {
                currentIndex.contains("CRUDE", ignoreCase = true) || currentIndex.contains("NATURALGAS", ignoreCase = true) -> "MCX"
                currentIndex.contains("SENSEX", ignoreCase = true) || currentIndex.contains("BANKEX", ignoreCase = true) -> "BSE"
                else -> "NSE"
            }

            OptionChainTabContent(
                viewModel = viewModel,
                exchange = exchange,
                indexName = currentIndex,
                onOpenOrderDialog = onOpenOrderDialog
            )
        }
    }
}

@Composable
fun IndexPill(index: String, currentIndex: String, onSelectIndex: (String) -> Unit) {
    val isSelected = index == currentIndex
    Surface(
        modifier = Modifier.clickable { onSelectIndex(index) },
        color = if (isSelected) PrimaryGold else DarkCard,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) PrimaryGold else DarkCardBorder)
    ) {
        Text(
            text = index,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.Black else TextWhite,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
