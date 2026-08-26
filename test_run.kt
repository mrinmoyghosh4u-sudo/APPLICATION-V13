import java.nio.ByteBuffer
import java.nio.ByteOrder

fun main() {
    val sampleBytes = byteArrayOf(
        0x20.toByte(), 0x00.toByte(), // Length 32 bytes (Little Endian)
        0x01.toByte(),                // Packet Type
        0x03.toByte(),                // Exchange Code 3 = BSE
        0x64.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // Token 100
        0x90.toByte(), 0x5F.toByte(), 0x01.toByte(), 0x00.toByte(), // LTP 900.00
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // Open
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // High
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), // Low
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()  // Close
    )
    val buffer = java.nio.ByteBuffer.wrap(sampleBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    val length = buffer.short.toInt() and 0xFFFF
    println("Length: \$length")
    val mode = buffer.get().toInt()
    println("Mode: \$mode")
    val exchangeCode = buffer.get().toInt()
    println("ExchangeCode: \$exchangeCode")
    val token = buffer.int
    println("Token: \$token")
    if (buffer.remaining() >= 4) {
        val ltp = buffer.int / 100.0
        println("LTP: \$ltp")
    }
}
