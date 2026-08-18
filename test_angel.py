import urllib.request
import json
url = "https://apiconnect.angelbroking.com/rest/secure/angelbroking/marketData/v1/optionChain"
req = urllib.request.Request(url, method="POST", headers={"Content-Type": "application/json"})
data = json.dumps({"exchange": "NSE", "symboltoken": "99926000", "expirydate": "28SEP2023"}).encode('utf-8')
try:
    with urllib.request.urlopen(req, data=data) as f:
        print(f.status)
        print(f.read().decode('utf-8')[:500])
except urllib.error.HTTPError as e:
    print(e.code)
    print(e.read().decode('utf-8')[:500])
except Exception as e:
    print(e)
