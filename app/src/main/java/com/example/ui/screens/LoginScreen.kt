package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.CrownLogo
import com.example.ui.theme.*

@Composable
fun LoginScreen(
    onConnectBroker: (String) -> Unit,
    onSkipLogin: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF040406),
                        Color(0xFF0A0902),
                        Color(0xFF020203)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // --- HEADER & LOGO BLOCK (Compact & Polished) ---
                Spacer(modifier = Modifier.height(4.dp))
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(PrimaryGold.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .border(1.2.dp, PrimaryGold.copy(alpha = 0.6f), CircleShape)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CrownLogo(size = 72.dp)
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "KING KHAN AI TRADE",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Motto Line with side gold accents
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(1.dp)
                            .background(PrimaryGold.copy(alpha = 0.6f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Trade ", color = ProfitGreen, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Like a ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Text("King ", color = LossRed, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Text("👑", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(1.dp)
                            .background(PrimaryGold.copy(alpha = 0.6f))
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- BROKER CONNECT CARD ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCard,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.8.dp, PrimaryGold.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header label
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(PrimaryGold.copy(alpha = 0.3f))
                            )
                            Text(
                                text = "SELECT BROKER TO AUTHENTICATE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGold,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(PrimaryGold.copy(alpha = 0.3f))
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // --- DHAN LOGIN BUTTON (PRIMARY EXECUTION) ---
                        BrokerLoginCard(
                            title = "LOGIN WITH DHAN",
                            subtitle = "⚡ Primary Order Execution & Dhan HQ API",
                            containerColor = Color(0xFF003D2C),
                            borderColor = Color(0xFF00875A),
                            testTag = "dhan_login_button",
                            iconContent = {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_dhan_logo),
                                    contentDescription = "Dhan Logo",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            onClick = { onConnectBroker("Dhan") }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // --- ANGEL ONE LOGIN BUTTON (PRIMARY MARKET DATA) ---
                        BrokerLoginCard(
                            title = "LOGIN WITH ANGEL ONE",
                            subtitle = "📊 Live Market Quotes & SmartAPI TOTP",
                            containerColor = Color(0xFF092B6B),
                            borderColor = Color(0xFF1E88E5),
                            testTag = "angel_login_button",
                            iconContent = {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_angel_one_logo),
                                    contentDescription = "Angel One Logo",
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            onClick = { onConnectBroker("Angel One") }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // --- M.STOCK LOGIN BUTTON (SECONDARY DATA) ---
                        BrokerLoginCard(
                            title = "LOGIN WITH m.STOCK",
                            subtitle = "📈 Secondary Data & Portfolio (Mirae Asset)",
                            containerColor = Color(0xFF8C1B1B),
                            borderColor = Color(0xFFE53935),
                            testTag = "mstock_login_button",
                            iconContent = {
                                Text(
                                    "m",
                                    color = Color(0xFFD32F2F),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black
                                )
                            },
                            onClick = { onConnectBroker("m.Stock") }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Features 4-in-a-row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FeatureBadge(icon = Icons.Outlined.Lock, title = "256-BIT\nENCRYPTED")
                            FeatureBadge(icon = Icons.Outlined.BarChart, title = "REAL TIME\nQUOTES")
                            FeatureBadge(icon = Icons.Outlined.FlashOn, title = "ALGO\nEXECUTION")
                            FeatureBadge(icon = Icons.Outlined.SupportAgent, title = "24/7\nSUPPORT")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- BOTTOM GOLDEN LINE < SKIP LOGIN > GOLDEN LINE ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSkipLogin() }
                    .padding(vertical = 10.dp)
                    .testTag("skip_login_button")
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, PrimaryGold.copy(alpha = 0.8f))
                            )
                        )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF14171C), RoundedCornerShape(18.dp))
                        .border(0.8.dp, PrimaryGold.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = PrimaryGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SKIP LOGIN",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = PrimaryGold,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PrimaryGold.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun BrokerLoginCard(
    title: String,
    subtitle: String,
    containerColor: Color,
    borderColor: Color,
    testTag: String,
    iconContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Logo Avatar Box
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    iconContent()
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = subtitle,
                        fontSize = 9.5.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Arrow Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FeatureBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(0.8.dp, PrimaryGold.copy(alpha = 0.7f), CircleShape)
                .background(DarkCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PrimaryGold,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            textAlign = TextAlign.Center,
            lineHeight = 10.sp
        )
    }
}
