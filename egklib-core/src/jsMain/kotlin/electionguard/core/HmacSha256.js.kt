package electionguard.core

import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

/**
 * This class maps to the @noble/hashes/hmac javascript library
 */
@JsModule("@noble/hashes/hmac")
@JsNonModule
external class NobleCryptoHmac {
    companion object {
        val hmac: HmacCreator
    }

    /**
     * This class maps to the HMAC class in hmac.d.ts in the @noble/hashes library
     */
    class HMAC {
        fun update(value: Uint8Array): HMAC
        fun digest(): Uint8Array
    }

    /**
     * This class maps to the hmac constant in hmac.d.ts in the @noble/hashes library
     */
    class HmacCreator {
        fun create(hash: Any, key: Uint8Array): HMAC
    }
}

/**
 * This class maps to the @noble/hashes/sha256 javascript library
 */
@JsModule("@noble/hashes/sha256")
@JsNonModule
external class NobleCryptoSha {
    companion object {
        val sha256: Any
    }
}

/**
 * The function HMAC( , ) shall be used to denote the HMAC-SHA-256 keyed Hash Message
 * Authentication Code (as defined in NIST PUB FIPS 198-1 (2) instantiated with SHA-256 (as
 * defined in NIST PUB FIPS 180-4 (3). HMAC takes as input a key k and a message m of
 * arbitrary length and returns a bit string HMAC(k, m) of length 256 bits.

 * (4) NIST (2008) The Keyed-Hash Message Authentication Code (HMAC). In: FIPS 198-1. https://csrc.nist.
 * gov/publications/detail/fips/198/1/final
 * (5) NIST (2015) Secure Hash Standard (SHS). In: FIPS 180-4. https://csrc.nist.gov/publications/detail/
 * fips/180/4/final
 *
 * spec 2.0.0, p.9.
 */
actual class HmacSha256 actual constructor(key: ByteArray) {
    private val hmac: NobleCryptoHmac.HMAC
    init {
        this.hmac = NobleCryptoHmac.hmac.create(NobleCryptoSha.sha256, Uint8Array(key.toTypedArray()))
    }
    actual fun update(ba: ByteArray) {
        hmac.update(Uint8Array(ba.toTypedArray()))
    }

    actual fun finish(): UInt256 {
        val digest = hmac.digest()
        val digestedBytes: List<Byte> = IntRange(0, digest.length - 1).map {
            digest[it]
        }
        return UInt256(digestedBytes.toByteArray())
    }
}
