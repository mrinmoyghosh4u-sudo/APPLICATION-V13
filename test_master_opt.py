import json
import urllib.request
with urllib.request.urlopen("https://margincalculator.angelbroking.com/OpenAPI_File/files/OpenAPIScripMaster.json") as url:
    data = json.loads(url.read().decode())
for d in data:
    if d['exch_seg'] == 'NFO' and d['name'] == 'NIFTY' and d['instrumenttype'] == 'OPTIDX':
        print(d)
        break
