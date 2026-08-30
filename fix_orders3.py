import re

filepath = "app/src/main/java/com/example/ui/screens/OrdersScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("""                Button(
                    
                    onClick = { onApply(selectedEx, selectedSide, selectedSort) },
                    modifier = Modifier.fillMaxWidth()
                )""", """                Button(
                    onClick = { onApply(selectedEx, selectedSide, selectedSort) },
                    modifier = Modifier.fillMaxWidth()
                ) { androidx.compose.material3.Text("Apply Filters", color = androidx.compose.ui.graphics.Color.White) }""")

with open(filepath, "w") as f:
    f.write(content)
