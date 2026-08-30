#!/bin/bash
sed -i '/composable("profile") {/i \
                            composable("health_autofix") {\
                                HealthAndAutoFixScreen(\
                                    viewModel = viewModel,\
                                    onNavigateBack = { navController.popBackStack() }\
                                )\
                            }\
' app/src/main/java/com/example/MainActivity.kt
