sed -i 's/val totalPnlPct = if (totalVal > 0) (totalPnl \/ totalVal) \* 100 else 0.0/val totalPnlPct = 0.0/' app/src/main/java/com/example/ui/screens/PortfolioScreen.kt
sed -i 's/userProfile.todaysPnlPercent/0.0/g' temp_portfolio.kt
