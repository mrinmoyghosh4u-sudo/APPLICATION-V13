fun parse(bytes: ByteArray) {
    val buffer = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.BIG_ENDIAN)
    val length = buffer.short.toInt()
    val respType = buffer.get().toInt()
    println("RespType: $respType")
}
