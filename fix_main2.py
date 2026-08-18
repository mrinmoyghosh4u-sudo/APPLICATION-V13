import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

nav_code = """                            composable("telegram_settings") {
                                TelegramSettingsScreen(
                                    botToken = telegramBotToken,
                                    chatId = telegramChatId,
                                    isAlertsEnabled = isTelegramAlertsEnabled,
                                    isTesting = isTelegramTesting,
                                    telegramResponseInfo = telegramResponseInfo,
                                    onBack = { navController.popBackStack() },
                                    onSaveSettings = { token, chatId, enabled ->
                                        viewModel.saveTelegramSettings(token, chatId, enabled)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Telegram settings updated")
                                        }
                                    },
                                    onTestConnection = { viewModel.testTelegramConnection() }
                                )
                            }
                            composable("diagnostics") {
                                com.example.ui.screens.DiagnosticsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }"""

text = re.sub(r'composable\("telegram_settings"\) \{.*?onTestConnection = \{ viewModel\.testTelegramConnection\(\) \}\n\s*\)\n\s*\}', nav_code, text, flags=re.DOTALL)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)
