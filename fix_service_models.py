import os

content = open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt").read()

# Fix OrderEntity
content = content.replace(
"""                    OrderEntity(
                        orderId = item.orderid ?: "",
                        symbol = item.tradingsymbol ?: "",
                        orderType = item.ordertype ?: "",
                        transactionType = item.transactiontype ?: "",
                        quantity = item.quantity?.toIntOrNull() ?: 0,
                        price = item.price?.toDoubleOrNull() ?: 0.0,
                        status = item.status ?: "",
                        timestamp = item.updatetime ?: ""
                    )""",
"""                    OrderEntity(
                        orderId = item.orderid ?: "",
                        symbol = item.tradingsymbol ?: "",
                        exchange = item.exchange ?: "NSE",
                        orderType = item.ordertype ?: "",
                        side = item.transactiontype ?: "",
                        qty = item.quantity?.toIntOrNull() ?: 0,
                        price = item.price?.toDoubleOrNull() ?: 0.0,
                        status = item.status ?: "",
                        time = item.updatetime ?: "",
                        lotSize = 1,
                        value = (item.price?.toDoubleOrNull() ?: 0.0) * (item.quantity?.toIntOrNull() ?: 0)
                    )"""
)

# Fix AngelPlaceOrderRequest
content = content.replace(
"""                variety = "NORMAL",
                tradingsymbol = order.symbol,
                symboltoken = instrumentMaster.resolveAngelToken(order.symbol, "NSE") ?: "",
                transactiontype = order.transactionType,
                exchange = "NSE",
                ordertype = order.orderType,
                producttype = "INTRADAY",
                duration = "DAY",
                price = order.price.toString(),
                squareoff = "0",
                stoploss = "0",
                quantity = order.quantity.toString()""",
"""                variety = "NORMAL",
                tradingSymbol = order.symbol,
                symbolToken = instrumentMaster.resolveAngelToken(order.symbol, "NSE") ?: "",
                transactionType = order.side,
                exchange = "NSE",
                orderType = order.orderType,
                productType = "INTRADAY",
                duration = "DAY",
                price = order.price.toString(),
                squareOff = "0",
                stopLoss = "0",
                quantity = order.qty.toString()"""
)

content = content.replace("orderid", "orderId")
content = content.replace("ordertype", "orderType")
content = content.replace("quantity = newQty.toString()", "quantity = newQty")

content = content.replace("val req = mapOf(\"variety\" to \"NORMAL\", \"orderid\" to orderId)", "val req = mapOf(\"variety\" to \"NORMAL\", \"orderId\" to orderId)")

# Fix PortfolioHoldingEntity
content = content.replace(
"""                    PortfolioHoldingEntity(
                        symbol = item.tradingsymbol ?: "",
                        quantity = item.quantity?.toIntOrNull() ?: 0,
                        averagePrice = item.averageprice?.toDoubleOrNull() ?: 0.0,
                        ltp = item.ltp?.toDoubleOrNull() ?: 0.0
                    )""",
"""                    PortfolioHoldingEntity(
                        symbol = item.tradingsymbol ?: "",
                        qty = item.quantity?.toIntOrNull() ?: 0,
                        avgPrice = item.averageprice?.toDoubleOrNull() ?: 0.0,
                        ltp = item.ltp?.toDoubleOrNull() ?: 0.0
                    )"""
)
content = content.replace(
"""                    PortfolioHoldingEntity(
                        symbol = item.tradingsymbol ?: "",
                        quantity = item.netqty?.toIntOrNull() ?: 0,
                        averagePrice = item.averageprice?.toDoubleOrNull() ?: 0.0,
                        ltp = item.ltp?.toDoubleOrNull() ?: 0.0
                    )""",
"""                    PortfolioHoldingEntity(
                        symbol = item.tradingsymbol ?: "",
                        qty = item.netqty?.toIntOrNull() ?: 0,
                        avgPrice = item.averageprice?.toDoubleOrNull() ?: 0.0,
                        ltp = item.ltp?.toDoubleOrNull() ?: 0.0
                    )"""
)

# Fix WatchlistItem
content = content.replace(
"""                        symbol = q.tradingSymbol?.toString() ?: "",
                        exchange = q.exchange?.toString() ?: "",
                        ltp = q.ltp?.toString()?.toDoubleOrNull() ?: 0.0,
                        change = q.netChange?.toString()?.toDoubleOrNull() ?: 0.0,
                        changePercent = q.percentChange?.toString()?.toDoubleOrNull() ?: 0.0,
                        token = q.symbolToken?.toString() ?: ""
                    )""",
"""                        symbol = q.tradingSymbol?.toString() ?: "",
                        exchange = q.exchange?.toString() ?: "",
                        ltp = q.ltp?.toString()?.toDoubleOrNull() ?: 0.0,
                        change = q.netChange?.toString()?.toDoubleOrNull() ?: 0.0,
                        changePercent = q.percentChange?.toString()?.toDoubleOrNull() ?: 0.0,
                        lotSize = 1,
                        isPositive = (q.netChange?.toString()?.toDoubleOrNull() ?: 0.0) >= 0
                    )"""
)

# Fix AngelOptionChainRequest
content = content.replace(
"""                exchange = instrument.exch_seg,
                symboltoken = instrument.token,
                expirydate = expiry""",
"""                exchange = instrument.exch_seg,
                symboltoken = instrument.token,
                expirydate = expiry"""
)

open("app/src/main/java/com/example/data/network/AngelOneBrokerService.kt", "w").write(content)

