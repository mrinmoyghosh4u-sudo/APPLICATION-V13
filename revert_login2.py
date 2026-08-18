import re

with open('app/src/main/java/com/example/ui/screens/LoginScreen.kt', 'r') as f:
    content = f.read()

# 1. Restore top skip button
content = re.sub(
    r'        Column\(',
    r'''        // --- TOP-RIGHT SKIP BUTTON ---
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

        Column(''',
    content,
    count=1
)

# 2. Restore Header & Logo Block
new_header = r'            // --- HEADER & LOGO BLOCK ---.*?            // --- BROKER CONNECT CARD ---'

old_header = '''            // --- HEADER & LOGO BLOCK ---
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

            // --- BROKER CONNECT CARD ---'''

content = re.sub(new_header, old_header, content, flags=re.DOTALL)

# 3. Restore Surface attributes
content = re.sub(
    r'            Surface\(\n                modifier = Modifier.fillMaxWidth\(\),\n                color = Color.Transparent,\n                shape = RoundedCornerShape\(20.dp\),\n                border = BorderStroke\(1.dp, PrimaryGold.copy\(alpha = 0.3f\)\)\n            \)',
    r'            Surface(\n                modifier = Modifier.fillMaxWidth(),\n                color = DarkCard,\n                shape = RoundedCornerShape(20.dp),\n                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.5f))\n            )',
    content
)

# 4. Restore Security Note and Bottom feature badges
new_sec = r'                    // Encrypted Connection Note.*?            // --- BOTTOM FEATURE BADGES & SKIP BUTTON ---'
old_sec = '''                    // Encrypted Connection Note
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
            // --- BOTTOM FEATURE BADGES & SKIP BUTTON ---'''

content = re.sub(new_sec, old_sec, content, flags=re.DOTALL)

# 5. Restore bottom skip button block
new_bot = r'            // --- BOTTOM FEATURE BADGES & SKIP BUTTON ---.*?\n        }\n    }\n}'
old_bot = '''            // --- BOTTOM FEATURE BADGES & SKIP BUTTON ---
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
}'''

content = re.sub(new_bot, old_bot, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/LoginScreen.kt', 'w') as f:
    f.write(content)
