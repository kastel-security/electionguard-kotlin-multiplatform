package electionguard.core

/**
 * External class to access the Web Crypto Api
 */
external interface Crypto {
    fun getRandomValues(typedArray: ByteArray): ByteArray
}

external interface Window {
    val crypto: Crypto
}

external val window: Window

/** Get "secure" random bytes from the native platform */
actual fun randomBytes(length: Int): ByteArray {
    return window.crypto.getRandomValues(ByteArray(length))
}
