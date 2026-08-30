#!/bin/bash
cat << 'INNER_EOF' > replacement.txt
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingMenuGridCard(
                            icon = Icons.Default.VolumeUp,
                            title = "SOUND & NOTIFICATIONS",
                            subtitle = "Push and sound alerts",
                            onClick = { showNotifPrefDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        SettingMenuGridCard(
                            icon = Icons.Default.MedicalServices,
                            title = "HEALTH & AUTO-FIX",
                            subtitle = "Run A-Z Check & Repair",
                            onClick = onNavigateToHealthAutoFix,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingMenuGridCard(
                            icon = Icons.Default.Speed,
                            title = "LIVE DIAGNOSTICS",
                            subtitle = "Ping & broker latency test",
                            onClick = onNavigateToDiagnostics,
                            modifier = Modifier.weight(1f)
                        )
                        SettingMenuGridCard(
                            icon = Icons.Default.CleaningServices,
                            title = "CLEAR APP CACHE",
                            subtitle = "Free memory & data",
                            onClick = { showClearCacheConfirmDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingMenuGridCard(
                            icon = Icons.Default.Gavel,
                            title = "TERMS & DISCLAIMER",
                            subtitle = "SEBI risk compliance",
                            onClick = { showTermsDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        SettingMenuGridCard(
                            icon = Icons.Default.Info,
                            title = "ABOUT KING KHAN",
                            subtitle = "v${com.example.BuildConfig.VERSION_NAME} Official",
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
INNER_EOF

# Replacing lines 880 to 908
sed -i '880,908c\
'$(cat replacement.txt | sed 's/$/\\/g')'' app/src/main/java/com/example/ui/screens/ProfileScreen.kt

