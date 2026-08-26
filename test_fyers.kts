@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.squareup.okhttp3:okhttp:4.11.0")

import okhttp3.*
import java.util.concurrent.CountDownLatch

val client = OkHttpClient()
val request = Request.Builder()
    .url("wss://socket.fyers.in/hsm/v1-5/prod")
    .build()

val latch = CountDownLatch(1)
client.newWebSocket(request, object : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        println("OPENED: ${response.code}")
        latch.countDown()
    }
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println("FAILED: ${t.message} code: ${response?.code}")
        latch.countDown()
    }
})
latch.await()
