package electionguard.core

/**
 * External class to access the Web Crypto Api
 */
private external interface Crypto {
    fun getRandomValues(typedArray: ByteArray): ByteArray
}

private external interface Window {
    val crypto: Crypto
}

private external val window: Window

private external fun require(module: String): dynamic

fun requireNode(module: String): dynamic = require(module)

/** Get "secure" random bytes from the native platform */
// this uses the Web Crypto API both in the browser and in Node.js
actual fun randomBytes(length: Int) =
    when (getPlatform()) {
        Platform.BROWSER -> window.crypto.getRandomValues(ByteArray(length))
        Platform.NODE -> requireNode("crypto")
            .getRandomValues(ByteArray(length)).unsafeCast<ByteArray>()
    }

fun getPlatform(): Platform =
    if (js("typeof window") != "undefined" && js("typeof document") != undefined)
        Platform.BROWSER
    else if (js("typeof process") != "undefined")
        Platform.NODE
    else
        throw Error("Unknown platform")

enum class Platform {
    BROWSER,
    NODE
}
