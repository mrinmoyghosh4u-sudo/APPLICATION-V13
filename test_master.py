import urllib.request
import json
print("Downloading...")
with urllib.request.urlopen("https://margincalculator.angelbroking.com/OpenAPI_File/files/OpenAPIScripMaster.json") as url:
    data = json.loads(url.read().decode())
print(f"Loaded {len(data)} instruments")
for d in data:
    if d['name'] in ['NIFTY', 'BANKNIFTY', 'FINNIFTY', 'MIDCPNIFTY'] and d['exch_seg'] == 'NSE':
        print(d)
