sed -i 's/onNavigateToProfile = { navController.navigate("profile") },//g' app/src/main/java/com/example/MainActivity.kt
sed -i 's/onOpenNotificationCenter = { },//g' app/src/main/java/com/example/MainActivity.kt
sed -i 's/onNavigateToDashboard = { navController.navigate("dashboard") }/onNavigateToDashboard = { navController.navigate("dashboard") },\nonNavigateToPositions = { navController.navigate("positions") },\nonNavigateToOrders = { navController.navigate("orders") }/g' app/src/main/java/com/example/MainActivity.kt
