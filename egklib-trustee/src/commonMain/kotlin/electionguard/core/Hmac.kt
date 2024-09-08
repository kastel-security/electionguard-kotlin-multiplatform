package electionguard.core

// only called from KeyCeremonyTrustee
// used for KDF and share encryption - see ElectionGuard spec 2.0, sec 3.2.2
fun hmacFunction(
    key: ByteArray,
    b1: Byte? = null,
    label: String? = null,
    b2: Byte? = null,
    context: String? = null,
    elements: List<Any>
): UInt256 {
    require(elements.isNotEmpty())
    val hmac = HmacSha256(key)
    b1?.let { hmac.addToHash(byteArrayOf(it)) }
    label?.let { hmac.addToHash(it) }
    b2?.let { hmac.addToHash(byteArrayOf(it)) }
    context?.let { hmac.addToHash(it) }
    elements.forEach { hmac.addToHash(it) }
    return hmac.finish()
}
