package electionguard.ballot

/**
 * A ByteArrayOutputStream wrapper for Kotlin/JS.
 */
actual class ByteArrayOutputStream actual constructor() {
    private val buffer = mutableListOf<Byte>()

    actual fun write(value: Int) {
        buffer.add(value.toByte())
    }

    actual fun write(ba: ByteArray) {
        buffer.addAll(ba.toList())
    }

    actual fun toByteArray(): ByteArray {
        return buffer.toByteArray()
    }
}

/**
 * A ByteArrayInputStream wrapper for Kotlin/JS.
 */
actual class ByteArrayInputStream actual constructor(private val buffer: ByteArray) {
    private var pos = 0

    actual fun read(): Int {
        return if (pos < buffer.size) {
            buffer[pos++].toInt() and 0xFF
        } else {
            -1 // Indicate end of stream
        }
    }

    actual fun read(ba: ByteArray): Int {
        if (pos >= buffer.size) return -1

        val len = ba.size.coerceAtMost(buffer.size - pos)
        for (i in 0 until len) {
            ba[i] = buffer[pos++]
        }

        return len
    }

}
