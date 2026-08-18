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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Security
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
        // --- TOP-RIGHT SKIP BUTTON ---
        TextButton(
            onClick = onSkipLogin,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 12.dp)
                .testTag("top_skip_login_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SKIP",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryGold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Skip Login",
                    tint = PrimaryGold,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- HEADER & LOGO BLOCK ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                // Crown emblem with outer golden radial ambient glow
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(150.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(PrimaryGold.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .border(1.5.dp, PrimaryGold.copy(alpha = 0.6f), CircleShape)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CrownLogo(size = 110.dp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "KING KHAN AI TRADE",
                    color = PrimaryGold,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Motto Line with side gold accents
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(1.dp)
                            .background(PrimaryGold.copy(alpha = 0.6f))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("TRADE ", color = ProfitGreen, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Text("LIKE A ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Text("KING 👑", color = PrimaryGold, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(1.dp)
                            .background(PrimaryGold.copy(alpha = 0.6f))
                    )
                }
            }

            // --- BROKER CONNECT CARD ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(PrimaryGold.copy(alpha = 0.3f))
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // --- DHAN LOGIN BUTTON (PRIMARY EXECUTION) ---
                    BrokerLoginCard(
                        title = "LOGIN WITH DHAN",
                        subtitle = "⚡ Primary Order Execution & Dhan HQ API",
                        badgeText = "PRIMARY EXECUTION",
                        badgeColor = ProfitGreen,
                        containerColor = Color(0xFF003D2C),
                        borderColor = Color(0xFF00875A),
                        testTag = "dhan_login_button",
                        iconContent = {
                            Image(
                                painter = painterResource(id = R.drawable.ic_dhan_logo),
                                contentDescription = "Dhan Logo",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = { onConnectBroker("Dhan") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- ANGEL ONE LOGIN BUTTON (PRIMARY MARKET DATA) ---
                    BrokerLoginCard(
                        title = "LOGIN WITH ANGEL ONE",
                        subtitle = "📊 Live Market Quotes & SmartAPI TOTP",
                        badgeText = "MARKET DATA",
                        badgeColor = Color(0xFF29B6F6),
                        containerColor = Color(0xFF092B6B),
                        borderColor = Color(0xFF1E88E5),
                        testTag = "angel_login_button",
                        iconContent = {
                            Image(
                                painter = painterResource(id = R.drawable.ic_angel_one_logo),
                                contentDescription = "Angel One Logo",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = { onConnectBroker("Angel One") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- M.STOCK LOGIN BUTTON (SECONDARY DATA) ---
                    BrokerLoginCard(
                        title = "LOGIN WITH m.STOCK",
                        subtitle = "📈 Secondary Data & Portfolio (Mirae Asset)",
                        badgeText = "PORTFOLIO & DATA",
                        badgeColor = PrimaryGold,
                        containerColor = Color(0xFF8C1B1B),
                        borderColor = Color(0xFFE53935),
                        testTag = "mstock_login_button",
                        iconContent = {
                            Text(
                                "m",
                                color = Color(0xFFD32F2F),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        },
                        onClick = { onConnectBroker("m.Stock") }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Encrypted Connection Note
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCardSecondary, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = PrimaryGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "OFFICIAL BROKER API & OAUTH 2.0",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                "Direct end-to-end token encryption • No password logging",
                                fontSize = 9.sp,
                                color = TextGray
                            )
                        }
                    }
                }
            }

            // --- BOTTOM FEATURE BADGES & SKIP BUTTON ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Highlights Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FeatureBadge(icon = Icons.Default.Shield, title = "256-BIT ENCRYPTED")
                    FeatureBadge(icon = Icons.Default.BarChart, title = "REAL TIME QUOTES")
                    FeatureBadge(icon = Icons.Default.FlashOn, title = "ALGO EXECUTION")
                    FeatureBadge(icon = Icons.Default.SupportAgent, title = "24/7 SUPPORT")
                }

                Spacer(modifier = Modifier.height(4.dp))

                // SKIP LOGIN / GUEST MODE BUTTON
                OutlinedButton(
                    onClick = onSkipLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("skip_login_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, PrimaryGold),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = PrimaryGold
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "SKIP LOGIN (EXPLORE APP)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = PrimaryGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrokerLoginCard(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
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
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Logo Avatar Box
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    iconContent()
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Arrow Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
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
        modifier = Modifier.width(76.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .border(1.dp, PrimaryGold.copy(alpha = 0.7f), CircleShape)
                .background(DarkCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PrimaryGold,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp
        )
    }
}



