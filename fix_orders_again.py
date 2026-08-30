import re

filepath = "app/src/main/java/com/example/ui/screens/OrdersScreen.kt"
try:
    with open(filepath, "r") as f:
        content = f.read()

    # Revert OutlinedTextField break
    content = content.replace(') { Text("Apply Filters", color = Color.Black) }', ')')

    # For Button that I also broke:
    content = content.replace(') { Text("Confirm Order Modification", color = Color.Black) }', ')')
    
    # We still need to give Button its content
    # Let's fix the first button (Confirm Order)
    old_button_code = """                Button(
                    onClick = {
                        val p = priceText.toDoubleOrNull() ?: order.price
                        val q = qtyText.toIntOrNull() ?: order.qty
                        val sl = slText.toDoubleOrNull() ?: order.stopLoss
                        val tg = targetText.toDoubleOrNull() ?: order.target
                        onConfirm(p, q, orderType, sl, tg)
                    },
                    modifier = Modifier.fillMaxWidth()
                )"""
    new_button_code = """                Button(
                    onClick = {
                        val p = priceText.toDoubleOrNull() ?: order.price
                        val q = qtyText.toIntOrNull() ?: order.qty
                        val sl = slText.toDoubleOrNull() ?: order.stopLoss
                        val tg = targetText.toDoubleOrNull() ?: order.target
                        onConfirm(p, q, orderType, sl, tg)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Confirm Order Modification", color = androidx.compose.ui.graphics.Color.White) }"""
    content = content.replace(old_button_code, new_button_code)
    
    old_apply_filters = """                Button(
                    onClick = { onApply(selectedEx, selectedSide, selectedSort) },
                    modifier = Modifier.fillMaxWidth()
                )"""
    new_apply_filters = """                Button(
                    onClick = { onApply(selectedEx, selectedSide, selectedSort) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Apply Filters", color = androidx.compose.ui.graphics.Color.White) }"""
    content = content.replace(old_apply_filters, new_apply_filters)

    with open(filepath, "w") as f:
        f.write(content)
except Exception as e:
    print(e)
