sed -i '/val prevClose =/d' app/src/main/java/com/example/viewmodel/MainViewModel.kt
sed -i '/val newChange =/d' app/src/main/java/com/example/viewmodel/MainViewModel.kt
sed -i '/val newChangePct =/d' app/src/main/java/com/example/viewmodel/MainViewModel.kt
sed -i 's/change = newChange,/change = live.change,/g' app/src/main/java/com/example/viewmodel/MainViewModel.kt
sed -i 's/changePercent = newChangePct,/changePercent = live.changePercent,/g' app/src/main/java/com/example/viewmodel/MainViewModel.kt
sed -i 's/isPositive = newChange >= 0/isPositive = live.change >= 0/g' app/src/main/java/com/example/viewmodel/MainViewModel.kt

sed -i 's/item.copy(/val prevClose = if (item.ltp > 0) item.ltp - item.change else 0.0\n                        val newChange = if (prevClose > 0) live.ltp - prevClose else live.change\n                        val newChangePct = if (prevClose > 0) (newChange \/ prevClose) * 100 else live.changePercent\n                        item.copy(/g' app/src/main/java/com/example/viewmodel/MainViewModel.kt
sed -i 's/change = live.change,/change = newChange,/g' app/src/main/java/com/example/viewmodel/MainViewModel.kt
sed -i 's/changePercent = live.changePercent,/changePercent = newChangePct,/g' app/src/main/java/com/example/viewmodel/MainViewModel.kt
sed -i 's/isPositive = live.change >= 0/isPositive = newChange >= 0/g' app/src/main/java/com/example/viewmodel/MainViewModel.kt
