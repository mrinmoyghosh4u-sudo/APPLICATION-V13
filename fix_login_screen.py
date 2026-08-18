import re

with open('app/src/main/java/com/example/ui/screens/LoginScreen.kt', 'r') as f:
    content = f.read()

# Remove top skip button
content = re.sub(
    r'        // --- TOP-RIGHT SKIP BUTTON ---.*?        Column\(',
    r'        Column(',
    content,
    flags=re.DOTALL
)

# Replace the Header & Logo block
new_header = '''            // --- HEADER & LOGO BLOCK ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Bull
                Image(
                    painter = painterResource(id = R.drawable.bull),
                    contentDescription = "Bull",
                    modifier = Modifier.weight(1f).height(100.dp),
                    contentScale = ContentScale.Fit
                )
                // Center Logo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(130.dp)
                ) {
                    CrownLogo(size = 110.dp)
                }
                // Bear
                Image(
                    painter = painterResource(id = R.drawable.bear),
                    contentDescription = "Bear",
                    modifier = Modifier.weight(1f).height(100.dp),
                    contentScale = ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "KING KHAN AI TRADE",
                color = PrimaryGold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Motto Line with side gold accents
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(1.dp)
                        .background(PrimaryGold.copy(alpha = 0.6f))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👑", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trade like a king", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Normal)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("👑", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(1.dp)
                        .background(PrimaryGold.copy(alpha = 0.6f))
                )
            }'''

content = re.sub(
    r'            // --- HEADER & LOGO BLOCK ---.*?            // --- BROKER CONNECT CARD ---',
    new_header + '\n\n            // --- BROKER CONNECT CARD ---',
    content,
    flags=re.DOTALL
)

# Update Surface attributes
content = re.sub(
    r'            Surface\(\s*modifier = Modifier\.fillMaxWidth\(\),\s*color = DarkCard,\s*shape = RoundedCornerShape\(20\.dp\),\s*border = BorderStroke\(1\.dp, PrimaryGold\.copy\(alpha = 0\.5f\)\)\s*\)',
    r'            Surface(\n                modifier = Modifier.fillMaxWidth(),\n                color = Color.Transparent,\n                shape = RoundedCornerShape(20.dp),\n                border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.3f))\n            )',
    content
)

# Update security Row
sec_old = r'                    // Encrypted Connection Note.*?// --- BOTTOM FEATURE BADGES & SKIP BUTTON ---'
sec_new = '''                    // Encrypted Connection Note
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent, RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = PrimaryGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "OFFICIAL BROKER API & OAUTH 2.0",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Direct end-to-end token encryption • No password logging\\nYour data is 100% secure with bank-grade protection.",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                lineHeight = 14.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Features
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FeatureBadge(icon = Icons.Outlined.Lock, title = "256-BIT\\nENCRYPTED")
                        FeatureBadge(icon = Icons.Outlined.BarChart, title = "REAL TIME\\nQUOTES")
                        FeatureBadge(icon = Icons.Outlined.FlashOn, title = "ALGO\\nEXECUTION")
                        FeatureBadge(icon = Icons.Outlined.SupportAgent, title = "24/7\\nSUPPORT")
                    }
                }
            }
            // --- BOTTOM FEATURE BADGES & SKIP BUTTON ---'''
content = re.sub(sec_old, sec_new, content, flags=re.DOTALL)


# Update bottom skip button
bot_old = r'            // --- BOTTOM FEATURE BADGES & SKIP BUTTON ---.*?\n        }\n    }\n}'
bot_new = '''            Spacer(modifier = Modifier.height(30.dp))
            
            // SKIP LOGIN BUTTON AT BOTTOM
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
                        .width(60.dp)
                        .height(1.dp)
                        .background(PrimaryGold.copy(alpha = 0.3f))
                )
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "SKIP LOGIN",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Skip",
                    tint = PrimaryGold,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(1.dp)
                        .background(PrimaryGold.copy(alpha = 0.3f))
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}'''
content = re.sub(bot_old, bot_new, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/LoginScreen.kt', 'w') as f:
    f.write(content)
