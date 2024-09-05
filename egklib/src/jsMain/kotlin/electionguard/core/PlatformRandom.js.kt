package electionguard.core

import electionguard.util.require
import js.typedarrays.Uint8Array

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
// this uses the Web Crypto API both in the browser and in Node.js
actual fun randomBytes(length: Int) =
    when (getPlatFormRandom()) {
        Platform.BROWSER -> window.crypto.getRandomValues(ByteArray(length))
        Platform.NODE -> require("crypto")
            .getRandomValues(Uint8Array(length)).unsafeCast<ByteArray>()
    }

fun getPlatFormRandom(): Platform =
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
