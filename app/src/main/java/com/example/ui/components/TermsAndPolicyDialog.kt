package com.example.ui.components

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

@Composable
fun TermsAndPolicyDialog(
    onDismiss: () -> Unit
) {
    var expanded1 by remember { mutableStateOf(true) }
    var expanded2 by remember { mutableStateOf(true) }
    var expanded3 by remember { mutableStateOf(true) }
    var expanded4 by remember { mutableStateOf(true) }
    var expanded5 by remember { mutableStateOf(true) }
    var expanded6 by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = DarkCard,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryGold)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(DarkCardSecondary, RoundedCornerShape(8.dp))
                            .border(1.dp, PrimaryGold, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = PrimaryGold, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("TERMS, PRIVACY & REGULATORY POLICY", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryGold)
                        Text("Please read all the sections carefully", fontSize = 11.sp, color = SecondaryGold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                // Section 1
                DialogPolicySectionCard(
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

                Spacer(modifier = Modifier.height(8.dp))

                // Section 2
                DialogPolicySectionCard(
                    icon = Icons.Default.Warning,
                    title = "2. RISK DISCLOSURE FOR DERIVATIVES & OPTIONS",
                    content = """Trading Futures and Options (F&O) carries extreme risk. As per SEBI studies, 9 out of 10 individual traders in equity Futures and Options segment incurred net losses.

Option buyer capital can drop to zero upon expiry if contracts close out-of-the-money. Leveraged positions incur rapid capital drawdown. Never trade with capital you cannot afford to lose completely.""",
                    isExpanded = expanded2,
                    onToggle = { expanded2 = !expanded2 }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Section 3
                DialogPolicySectionCard(
                    icon = Icons.Default.Lock,
                    title = "3. PRIVACY POLICY & CREDENTIALS SECURITY",
                    content = """KING KHAN AI TRADE does not store broker passwords, PINs or sensitive API secrets on external servers. All authentication tokens generated via official OAuth or API connections are stored strictly locally on your Android device using Android KeyStore and EncryptedSharedPreferences.

Personal data is never sold, leased, or transmitted to third parties.""",
                    isExpanded = expanded3,
                    onToggle = { expanded3 = !expanded3 }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Section 4
                DialogPolicySectionCard(
                    icon = Icons.Default.Handshake,
                    title = "4. BROKER / API INTEGRATION DISCLAIMER",
                    content = """This application connects to official APIs provided by Dhan (Moneylicious Securities) and Angel One (SmartAPI).

KING KHAN AI TRADE is an independent client software interface and is not affiliated with, endorsed by, or sponsored by Moneylicious Securities Pvt. Ltd. or Angel One Ltd.""",
                    isExpanded = expanded4,
                    onToggle = { expanded4 = !expanded4 }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Section 5
                DialogPolicySectionCard(
                    icon = Icons.Default.TrendingUp,
                    title = "5. DATA & MARKET DATA DISCLAIMER",
                    content = """Market feeds, LTP, Option Chain metrics, Greeks, and order execution times depend on official broker socket connections and internet network stability.

Latency or socket disconnects during volatility may delay live price updates.""",
                    isExpanded = expanded5,
                    onToggle = { expanded5 = !expanded5 }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Section 6
                DialogPolicySectionCard(
                    icon = Icons.Default.Person,
                    title = "6. USER RESPONSIBILITY & ACCOUNT SAFEGUARDS",
                    content = """You are solely responsible for maintaining the secrecy of your device PIN, passcode, and biometric lock.

You acknowledge full liability for all orders placed through your connected broker account.""",
                    isExpanded = expanded6,
                    onToggle = { expanded6 = !expanded6 }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold)
                ) {
                    Text("CLOSE & ACCEPT", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DialogPolicySectionCard(
    icon: ImageVector,
    title: String,
    content: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() },
        color = DarkCardSecondary,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF221A0A), CircleShape)
                        .border(1.dp, SecondaryGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SecondaryGold,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = SecondaryGold,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Divider(color = DarkCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = content,
                    fontSize = 10.5.sp,
                    color = TextWhite,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
