package electionguard.model

/**
 * A ByteArrayOutputStream wrapper for the JVM.
 */
actual class ByteArrayOutputStream actual constructor() {
    private val byteArrayOutputStream = java.io.ByteArrayOutputStream()

    actual fun write(value: Int) = byteArrayOutputStream.write(value)

    actual fun write(ba: ByteArray) = byteArrayOutputStream.write(ba)

    actual fun toByteArray(): ByteArray = byteArrayOutputStream.toByteArray()
}

/**
 * A ByteArrayInputStream wrapper for the JVM.
 */
actual class ByteArrayInputStream actual constructor(buffer: ByteArray) {
    private val byteArrayInputStream = java.io.ByteArrayInputStream(buffer)

    actual fun read() = byteArrayInputStream.read()

    actual fun read(ba: ByteArray) = byteArrayInputStream.read(ba)
}
