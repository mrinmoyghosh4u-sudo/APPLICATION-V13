import urllib.request
import json
import os

if not os.path.exists("OpenAPIScripMaster.json"):
    print("Downloading master file...")
    urllib.request.urlretrieve("https://margincalculator.angelbroking.com/OpenAPI_File/files/OpenAPIScripMaster.json", "OpenAPIScripMaster.json")

print("Parsing...")
data = json.load(open("OpenAPIScripMaster.json"))
index_map = {}
for inst in data:
    exch = inst.get("exch_seg", "")
    instType = inst.get("instrumenttype", "")
    name = inst.get("name", "")
    symbol = inst.get("symbol", "")
    token = inst.get("token", "")
    
    if exch in ("NSE", "BSE", "MCX") and (instType == "" or instType == "AMXIDX" or "FUT" in instType or "IDX" in instType) and (name in ("NIFTY", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY", "SENSEX", "BANKEX") or symbol.startswith("CRUDEOIL")):
        isIndex = (instType == "AMXIDX") or (exch == "BSE" and instType == "") or (exch == "MCX" and "FUT" in instType)
        if isIndex or name not in index_map:
            if name in ("NIFTY", "BANKNIFTY", "FINNIFTY", "MIDCPNIFTY", "SENSEX", "BANKEX"):
                index_map[name] = inst
        if symbol.startswith("CRUDEOIL"):
            if "CRUDEOILM" in symbol:
                if "CRUDEOIL M" not in index_map: index_map["CRUDEOIL M"] = inst
            elif symbol.startswith("CRUDEOIL") and "M" not in symbol:
                if "CRUDEOIL" not in index_map: index_map["CRUDEOIL"] = inst

print(json.dumps(index_map, indent=2))
