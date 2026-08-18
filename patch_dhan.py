import re

with open('/app/applet/app/src/main/java/com/example/data/network/DhanBrokerService.kt', 'r') as f:
    content = f.read()

# Add import if not present
if "import com.example.data.model.OptionChainInstrumentMaster" not in content:
    content = content.replace("import com.example.data.model.OptionStrikeItem", "import com.example.data.model.OptionStrikeItem\nimport com.example.data.model.OptionChainInstrumentMaster")

pattern = r'val request = DhanOptionChainRequest\(\s*underlyingScrip = 13,\s*underlyingSeg = "IDX_I",\s*expiry = expiry\s*\)'

replacement = """val instrument = OptionChainInstrumentMaster.getInstrument(symbol)
            val scrip = instrument?.underlyingScrip ?: 13
            val seg = instrument?.underlyingSeg ?: "IDX_I"
            val request = DhanOptionChainRequest(
                underlyingScrip = scrip,
                underlyingSeg = seg,
                expiry = expiry
            )"""

new_content = re.sub(pattern, replacement, content)

if new_content != content:
    with open('/app/applet/app/src/main/java/com/example/data/network/DhanBrokerService.kt', 'w') as f:
        f.write(new_content)
    print("DhanBrokerService Patched!")
else:
    print("DhanBrokerService Patch failed!")

