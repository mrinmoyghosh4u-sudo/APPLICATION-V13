import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

text = text.replace(
    "onNavigateToTelegramSettings = { navController.navigate(\"telegram_settings\") },",
    "onNavigateToTelegramSettings = { navController.navigate(\"telegram_settings\") },\n                                            onNavigateToDiagnostics = { navController.navigate(\"diagnostics\") },"
)

nav_code = """
                        composable("telegram_settings") {
                            com.example.ui.screens.TelegramSettingsScreen(
                                userProfile = userProfile,
                                onBack = { navController.popBackStack() },
                                onSave = { token, chatId ->
                                    viewModel.saveTelegramSettings(token, chatId)
                                }
                            )
                        }
                        
                        composable("diagnostics") {
                            com.example.ui.screens.DiagnosticsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
"""

text = re.sub(r'composable\("telegram_settings"\)\s*\{[^\}]+\n\s*\}[^\}]+\n\s*\}', nav_code, text)
text = text.replace("""                        composable("telegram_settings") {
                            com.example.ui.screens.TelegramSettingsScreen(
                                userProfile = userProfile,
                                onBack = { navController.popBackStack() },
                                onSave = { token, chatId ->
                                    viewModel.saveTelegramSettings(token, chatId)
                                }
                            )
                        }""", nav_code)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)

