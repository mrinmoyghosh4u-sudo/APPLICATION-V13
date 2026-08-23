package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CrownLogo
import com.example.ui.theme.*
import com.example.util.AppPreferences

@Composable
fun DisclaimerScreen(
    appPreferences: AppPreferences? = null,
    onAgreeAndContinue: () -> Unit
) {
    var isChecked by remember { mutableStateOf(false) }

    var expanded1 by remember { mutableStateOf(true) }
    var expanded2 by remember { mutableStateOf(true) }
    var expanded3 by remember { mutableStateOf(true) }
    var expanded4 by remember { mutableStateOf(true) }
    var expanded5 by remember { mutableStateOf(true) }
    var expanded6 by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0A04),
                        DarkBackground,
                        Color(0xFF140F05)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Branding Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CrownLogo(size = 54.dp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "KING KHAN AI TRADE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                com.example.ui.components.KingKhanTagline(fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Main Disclaimer Card Frame
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                color = DarkCard.copy(alpha = 0.95f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryGold)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Card Title Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCardSecondary)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = PrimaryGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TERMS, PRIVACY & REGULATORY POLICY",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Please read all the sections carefully before continuing.",
                            fontSize = 11.sp,
                            color = TextWhite.copy(alpha = 0.85f)
                        )
                    }

                    Divider(color = DarkCardBorder, thickness = 1.dp)

                    // Scrollable Sections Area
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DisclaimerSectionCard(
                            icon = Icons.Default.School,
                            title = "1. EDUCATIONAL & PERSONAL USE DISCLAIMER",
                            content = """KING KHAN AI TRADE is an educational and personal-use trading application. The application is not registered with SEBI as an investment adviser, research analyst, portfolio manager, or other regulated intermediary.

The application provides software tools, market information, analytics and technical/algorithmic features for educational, research and personal-use purposes only. Any signals, indicators, calculations or analysis displayed by the application are not investment advice, recommendations, guarantees or assurances of profit.

Users are solely responsible for their own trading and investment decisions. Trading in securities and derivatives involves substantial risk, including the possibility of significant financial loss.

Users should independently verify all information and, where appropriate, consult a SEBI-registered professional before making financial decisions.

KING KHAN AI TRADE does not guarantee accuracy, completeness, availability, uninterrupted service or profitability of any strategy, signal or market data.""",
                            isExpanded = expanded1,
                            onToggle = { expanded1 = !expanded1 }
                        )

                        DisclaimerSectionCard(
                            icon = Icons.Default.Warning,
                            title = "2. RISK DISCLOSURE FOR DERIVATIVES & OPTIONS",
                            content = """Trading Futures and Options (F&O) carries extreme risk. As per SEBI studies, 9 out of 10 individual traders in equity Futures and Options segment incurred net losses.

Option buyer capital can drop to zero upon expiry if contracts close out-of-the-money. Leveraged positions incur rapid capital drawdown. Never trade with capital you cannot afford to lose completely.""",
                            isExpanded = expanded2,
                            onToggle = { expanded2 = !expanded2 }
                        )

                        DisclaimerSectionCard(
                            icon = Icons.Default.Lock,
                            title = "3. PRIVACY POLICY & CREDENTIALS SECURITY",
                            content = """KING KHAN AI TRADE does not store broker passwords, PINs or sensitive API secrets on external servers. All authentication tokens generated via official OAuth or API connections are stored strictly locally on your Android device using Android KeyStore and EncryptedSharedPreferences.

Personal data is never sold, leased, or transmitted to third parties.""",
                            isExpanded = expanded3,
                            onToggle = { expanded3 = !expanded3 }
                        )

                        DisclaimerSectionCard(
                            icon = Icons.Default.Handshake,
                            title = "4. BROKER / API INTEGRATION DISCLAIMER",
                            content = """This application connects to official APIs provided by Dhan (Moneylicious Securities) and Angel One (SmartAPI).

KING KHAN AI TRADE is an independent client software interface and is not affiliated with, endorsed by, or sponsored by Moneylicious Securities Pvt. Ltd. or Angel One Ltd.""",
                            isExpanded = expanded4,
                            onToggle = { expanded4 = !expanded4 }
                        )

                        DisclaimerSectionCard(
                            icon = Icons.Default.TrendingUp,
                            title = "5. DATA & MARKET DATA DISCLAIMER",
                            content = """Market feeds, LTP, Option Chain metrics, Greeks, and order execution times depend on official broker socket connections and internet network stability.

Latency or socket disconnects during volatility may delay live price updates.""",
                            isExpanded = expanded5,
                            onToggle = { expanded5 = !expanded5 }
                        )

                        DisclaimerSectionCard(
                            icon = Icons.Default.Person,
                            title = "6. USER RESPONSIBILITY & ACCOUNT SAFEGUARDS",
                            content = """You are solely responsible for maintaining the secrecy of your device PIN, passcode, and biometric lock.

You acknowledge full liability for all orders placed through your connected broker account.""",
                            isExpanded = expanded6,
                            onToggle = { expanded6 = !expanded6 }
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            // Bottom Action Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Checkbox Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isChecked = !isChecked }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimaryGold,
                            uncheckedColor = SecondaryGold,
                            checkmarkColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "I have read and understood the Risk Disclaimer.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Button flanked by Bull & Bear graphics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    GoldBullGraphic(modifier = Modifier.size(46.dp, 34.dp))

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = {
                            if (isChecked) {
                                appPreferences?.setDisclaimerAccepted(true)
                                onAgreeAndContinue()
                            }
                        },
                        enabled = isChecked,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGold,
                            disabledContainerColor = DarkCardSecondary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = "<<<<   I AGREE & CONTINUE   >>>>",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isChecked) Color.Black else TextGray
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    GoldBearGraphic(modifier = Modifier.size(46.dp, 34.dp))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Footer
                Text(
                    text = "© 2024 KING KHAN AI TRADE. All Rights Reserved.",
                    fontSize = 10.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DisclaimerSectionCard(
    icon: ImageVector,
    title: String,
    content: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggle() },
        color = DarkCardSecondary,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gold Icon Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF221A0A), CircleShape)
                        .border(1.dp, SecondaryGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SecondaryGold,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryGold,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = SecondaryGold,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = DarkCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = content,
                    fontSize = 11.sp,
                    color = TextWhite,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun GoldBullGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.9f, size.height * 0.3f)
            quadraticTo(size.width * 0.7f, size.height * 0.2f, size.width * 0.6f, size.height * 0.35f)
            quadraticTo(size.width * 0.4f, size.height * 0.15f, size.width * 0.2f, size.height * 0.3f)
            lineTo(size.width * 0.05f, size.height * 0.5f)
            lineTo(size.width * 0.1f, size.height * 0.85f)
            lineTo(size.width * 0.2f, size.height * 0.85f)
            lineTo(size.width * 0.3f, size.height * 0.6f)
            lineTo(size.width * 0.45f, size.height * 0.85f)
            lineTo(size.width * 0.55f, size.height * 0.85f)
            lineTo(size.width * 0.65f, size.height * 0.5f)
            lineTo(size.width * 0.85f, size.height * 0.45f)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(PrimaryGold, SecondaryGold, Color(0xFFB8860B))
            )
        )
    }
}

@Composable
private fun GoldBearGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.1f, size.height * 0.35f)
            lineTo(size.width * 0.3f, size.height * 0.2f)
            quadraticTo(size.width * 0.5f, size.height * 0.25f, size.width * 0.7f, size.height * 0.35f)
            lineTo(size.width * 0.95f, size.height * 0.5f)
            lineTo(size.width * 0.85f, size.height * 0.85f)
            lineTo(size.width * 0.75f, size.height * 0.85f)
            lineTo(size.width * 0.6f, size.height * 0.6f)
            lineTo(size.width * 0.45f, size.height * 0.85f)
            lineTo(size.width * 0.35f, size.height * 0.85f)
            lineTo(size.width * 0.25f, size.height * 0.5f)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(PrimaryGold, SecondaryGold, Color(0xFFB8860B))
            )
        )
    }
}
