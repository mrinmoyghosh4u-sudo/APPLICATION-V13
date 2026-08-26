import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response

fun main() {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("wss://api.fyers.in/socket/v2/data/")
        .build()
        
    client.newWebSocket(request, object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            println("OPENED: " + response.code)
            System.exit(0)
        }
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            println("FAILED: " + t.message + " code: " + response?.code)
            System.exit(1)
        }
    })
    Thread.sleep(5000)
}
