sed -i 's/val pnlPercent: Double/val pnlPercent: Double,\n    val realizedPnl: Double = 0.0,\n    val unrealizedPnl: Double = 0.0/' app/src/main/java/com/example/data/model/TradingEntities.kt
