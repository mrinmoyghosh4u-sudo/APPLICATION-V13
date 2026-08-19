import json

with open('app/src/main/java/com/example/data/network/InstrumentMasterService.kt', 'r') as f:
    print(f.read().split("resolveIndexToken")[1][:500])
