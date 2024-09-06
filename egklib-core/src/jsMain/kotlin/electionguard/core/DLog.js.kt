package electionguard.core

/**
 * Construct a DLog implementation for the given `base` element.
 *
 * For computing DLog with group generator as the base, see [GroupContext.dLogG].
 *
 * For computing DLog with a public key as the base, see [ElGamalPublicKey.dLog].
 */
actual fun dLoggerOf(base: ElementModP) = DLog(base)

// TODO make this settable. The maximum vote allowed for the tally.
private const val MAX_DLOG: Int = 100_000

/** General-purpose discrete-log engine. */
actual class DLog actual constructor(actual val base: ElementModP) {

    // for the browser implementation, we do not care about concurrency
    private val dLogMapping: MutableMap<ElementModP, Int> =
        mutableMapOf<ElementModP, Int>().apply {
            this[base.context.ONE_MOD_P] = 0
        }

    private var dLogMaxElement = base.context.ONE_MOD_P
    private var dLogMaxExponent = 0

    /**
     * Given an element x for which there exists an e, such that (base)^e = x, this will find e,
     * so long as e is less than [maxResult], which if unspecified defaults to a platform-specific
     * value designed not to consume too much memory (perhaps 10 million). This will consume O(e)
     * time, the first time, after which the results are memoized for all values between 0 and e,
     * for better future performance.
     *
     * If the result is not found, `null` is returned.
     */
    actual fun dLog(input: ElementModP, maxResult: Int): Int? =
        if (input in dLogMapping) {
            dLogMapping[input]
        } else {
            var error = false
            val dlogMax = if (maxResult < 0) MAX_DLOG else maxResult

            while (input != dLogMaxElement) {
                if (dLogMaxExponent++ > dlogMax) {
                    error = true
                    break
                } else {
                    dLogMaxElement *= base
                    dLogMapping[dLogMaxElement] = dLogMaxExponent
                }
            }

            if (error) null else dLogMaxExponent
        }
}
