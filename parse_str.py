import re

content = open("OpenAPIScripMaster.json", errors='ignore').read()

print("File size:", len(content))
nifty_match = re.search(r'\{[^\}]*"name":"NIFTY","exch_seg":"NSE","instrumenttype":"AMXIDX"[^\}]*\}', content)
print("NIFTY:", nifty_match.group(0) if nifty_match else "NOT FOUND")

banknifty_match = re.search(r'\{[^\}]*"name":"BANKNIFTY","exch_seg":"NSE","instrumenttype":"AMXIDX"[^\}]*\}', content)
print("BANKNIFTY:", banknifty_match.group(0) if banknifty_match else "NOT FOUND")

sensex_match = re.search(r'\{[^\}]*"name":"SENSEX","exch_seg":"BSE","instrumenttype":""[^\}]*\}', content)
print("SENSEX:", sensex_match.group(0) if sensex_match else "NOT FOUND")

finnifty_match = re.search(r'\{[^\}]*"name":"FINNIFTY"[^\}]*\}', content)
print("FINNIFTY:", finnifty_match.group(0) if finnifty_match else "NOT FOUND")

