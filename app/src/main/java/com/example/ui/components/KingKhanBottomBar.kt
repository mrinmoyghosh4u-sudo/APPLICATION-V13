package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Market : BottomNavItem("market", "Market", Icons.Filled.Timeline, Icons.Outlined.Timeline)
    object AISignals : BottomNavItem("ai_signals", "AI Signals", Icons.Filled.SmartToy, Icons.Outlined.SmartToy)
    object Orders : BottomNavItem("orders", "Orders", Icons.Filled.Receipt, Icons.Outlined.Receipt)
    object Algo : BottomNavItem("algo", "Algo", Icons.Filled.Memory, Icons.Outlined.Memory)
    object Profile : BottomNavItem("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun KingKhanBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Market,
        BottomNavItem.AISignals,
        BottomNavItem.Orders,
        BottomNavItem.Algo,
        BottomNavItem.Profile
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF0D0D0D),
        tonalElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(color = Color(0xFF222222), thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route || (item.route == "profile" && currentRoute == "portfolio")
                    val color = if (isSelected) PrimaryGold else TextGray

                    Column(
                        modifier = Modifier
                            .clickable { onNavigate(item.route) }
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.title,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = color,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
