sed -i 's/val value = price \* qty/val value = price.toDouble() \* qty.toDouble()/g' app/src/main/java/com/example/data/network/AngelOneBrokerService.kt
