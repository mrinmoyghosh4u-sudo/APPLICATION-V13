import re

with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "r") as f:
    content = f.read()

target = """                        SettingMenuGridCard(
                            icon = Icons.Default.Info,
                            title = "ABOUT KING KHAN",
                            subtitle = "v${com.example.BuildConfig.VERSION_NAME} Official",
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }"""

replacement = """                        SettingMenuGridCard(
                            icon = Icons.Default.Info,
                            title = "ABOUT KING KHAN",
                            subtitle = "v${com.example.BuildConfig.VERSION_NAME} Official",
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingMenuGridCard(
                            icon = Icons.Default.Update,
                            title = "CHECK FOR UPDATES",
                            subtitle = "Version management",
                            onClick = { showUpdateDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }"""

content = content.replace(target, replacement)

# ensure Update import is there
if "import androidx.compose.material.icons.filled.Update" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Info", "import androidx.compose.material.icons.filled.Info\nimport androidx.compose.material.icons.filled.Update")

with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "w") as f:
    f.write(content)

