import websocket
import json
import os
import time

app_id = "B5B7U2W9P0-100" 
token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9..." # Need a real token for Fyers? No, I don't have one.

def on_message(ws, message):
    print("Received:", message)

def on_error(ws, error):
    print("Error:", error)

def on_close(ws, close_status_code, close_msg):
    print("Closed:", close_status_code, close_msg)

def on_open(ws):
    print("Opened")
    payload = {
        "symbol": ["NSE:NIFTY50-INDEX"],
        "type": "lite"
    }
    ws.send(json.dumps(payload))

if __name__ == "__main__":
    websocket.enableTrace(True)
    ws = websocket.WebSocketApp("wss://api.fyers.in/socket/v2/data/",
                              header={"Authorization": f"{app_id}:{token}"},
                              on_open=on_open,
                              on_message=on_message,
                              on_error=on_error,
                              on_close=on_close)
    ws.run_forever()
