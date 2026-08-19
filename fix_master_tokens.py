content = open("app/src/main/java/com/example/data/network/InstrumentMasterService.kt").read()

content = content.replace('"NIFTY" to Instrument("26000"', '"NIFTY" to Instrument("99926000"')
content = content.replace('"BANKNIFTY" to Instrument("26009"', '"BANKNIFTY" to Instrument("99926009"')
content = content.replace('"FINNIFTY" to Instrument("26037"', '"FINNIFTY" to Instrument("99926037"')
content = content.replace('"MIDCPNIFTY" to Instrument("26074"', '"MIDCPNIFTY" to Instrument("99926074"')
content = content.replace('"SENSEX" to Instrument("99926000"', '"SENSEX" to Instrument("99919000"')
content = content.replace('"BANKEX" to Instrument("99926009"', '"BANKEX" to Instrument("99919009"')

open("app/src/main/java/com/example/data/network/InstrumentMasterService.kt", "w").write(content)
