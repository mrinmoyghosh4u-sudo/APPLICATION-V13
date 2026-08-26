import java.io.File
import java.util.regex.Pattern

def main():
    f = open("app/src/main/java/com/example/data/network/FyersMarketDataService.kt", "r")
    c = f.read()
    f.close()
    print("Has webSocket.send:", "webSocket.send" in c)
    print("Has webSocket?.send:", "webSocket?.send" in c)
    for i, line in enumerate(c.splitlines()):
        if "webSocket.send(" in line:
            print("Line", i+1, ":", line)

main()
