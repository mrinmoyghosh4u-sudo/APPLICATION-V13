with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "onClearResponse = {" in line:
        # Find the end of this composable
        for j in range(i, len(lines)):
            if "                            }" in lines[j] and "                        }" in lines[j+1]:
                lines[j] = """                            }
                            
                            composable("diagnostics") {
                                com.example.ui.screens.DiagnosticsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }
"""
                break
        break

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.writelines(lines)
