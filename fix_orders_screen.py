import re

filepath = "app/src/main/java/com/example/ui/screens/OrdersScreen.kt"
try:
    with open(filepath, "r") as f:
        content = f.read()

    # Fix line 1289 (Button missing content)
    # The previous code for the button was:
    # Button(
    #     onClick = { ... },
    #     modifier = Modifier.fillMaxWidth()
    # )
    # which is missing { Text("...") }
    content = content.replace("modifier = Modifier.fillMaxWidth()\n                )\n            }\n        }\n    }\n}\n", 'modifier = Modifier.fillMaxWidth()\n                ) { Text("Confirm Order Modification", color = Color.Black) }\n            }\n        }\n    }\n}\n')

    # Fix GoldButton -> Button
    content = content.replace("GoldButton(", "Button(")
    content = content.replace('text = "Apply Filters",', "")
    content = content.replace("onClick = { onApply(selectedEx, selectedSide, selectedSort) },", "onClick = { onApply(selectedEx, selectedSide, selectedSort) },")
    content = content.replace("modifier = Modifier.fillMaxWidth()\n                )", 'modifier = Modifier.fillMaxWidth()\n                ) { Text("Apply Filters", color = Color.Black) }')

    with open(filepath, "w") as f:
        f.write(content)
except Exception as e:
    print(e)
