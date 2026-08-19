import json

data = json.load(open("OpenAPIScripMaster.json"))
nifty_index = [x for x in data if x.get('name') == 'NIFTY' and x.get('exch_seg') == 'NSE' and x.get('instrumenttype') == 'AMXIDX']
print("NIFTY AMXIDX:", nifty_index)

nifty_options = [x for x in data if x.get('name') == 'NIFTY' and x.get('instrumenttype') == 'OPTIDX']
print("NIFTY OPTIDX Count:", len(nifty_options))

banknifty = [x for x in data if x.get('name') == 'BANKNIFTY' and x.get('exch_seg') == 'NSE' and x.get('instrumenttype') == 'AMXIDX']
print("BANKNIFTY AMXIDX:", banknifty)

finnifty = [x for x in data if x.get('name') == 'FINNIFTY' and x.get('exch_seg') == 'NSE' and x.get('instrumenttype') == 'AMXIDX']
print("FINNIFTY AMXIDX:", finnifty)

midcpnifty = [x for x in data if x.get('name') == 'MIDCPNIFTY' and x.get('exch_seg') == 'NSE' and x.get('instrumenttype') == 'AMXIDX']
print("MIDCPNIFTY AMXIDX:", midcpnifty)

sensex = [x for x in data if x.get('name') == 'SENSEX' and x.get('exch_seg') == 'BSE']
print("SENSEX Count:", len(sensex))
for s in sensex:
    if s.get('instrumenttype') == '':
        print("SENSEX empty instType:", s)

crudeoil = [x for x in data if x.get('symbol', '').startswith('CRUDEOIL') and x.get('exch_seg') == 'MCX' and 'FUT' in x.get('instrumenttype', '')]
print("CRUDEOIL FUT Count:", len(crudeoil))

