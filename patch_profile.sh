#!/bin/bash
sed -i '/Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {/i \
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {\
                        SettingMenuGridCard(\
                            icon = Icons.Default.MedicalServices,\
                            title = "HEALTH & AUTO-FIX",\
                            subtitle = "Run A-Z Check & Repair",\
                            onClick = onNavigateToHealthAutoFix,\
                            modifier = Modifier.weight(1f)\
                        )\
                        Spacer(modifier = Modifier.weight(1f))\
                    }' app/src/main/java/com/example/ui/screens/ProfileScreen.kt
