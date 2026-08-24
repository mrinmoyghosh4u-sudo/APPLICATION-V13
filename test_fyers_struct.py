import urllib.request
url = "https://raw.githubusercontent.com/FyersDev/fyers-api-v3-python-websocket/main/fyers_apiv3/FyersSocket.py"
try:
    with urllib.request.urlopen(url) as response:
        print(response.read().decode('utf-8'))
except:
    print("Could not fetch")
