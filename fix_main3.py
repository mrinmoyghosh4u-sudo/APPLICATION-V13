with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

text = text.replace('onTestConnection = { viewModel.testTelegramConnection() }\n                                )\n                            }', 
'''onTestConnection = { viewModel.testTelegramConnection() }
                                )
                            }
                            
                            composable("diagnostics") {
                                com.example.ui.screens.DiagnosticsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }''')

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)
