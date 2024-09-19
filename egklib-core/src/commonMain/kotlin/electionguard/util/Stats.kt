package electionguard.util

import kotlin.math.min

/** Keep track of timing stats. Thread-safe. */
expect class Stats() {
    fun of(who: String, thing: String = "decryption", what: String = "ballot"): Stat
    fun show(who: String)
    fun get(who: String) : Stat?
    fun show(len: Int = 3)
    fun count() : Int
    fun showLines(len: Int = 3): List<String>
}

expect class Stat(thing: String, what: String) {
    val thing: String
    val what: String
    fun accum(amount: Long, nthings: Int)
    fun accum(): Long
    fun nthings(): Int
    fun count(): Int
    fun copy(accum: Long, nthings: Int = nthings(), count: Int = count()): Stat
}

fun Stat.show(len: Int = 3): String {
    val perThing = if (nthings() == 0) 0.0 else accum().toDouble() / nthings()
    val perWhat = if (count() == 0) 0.0 else accum().toDouble() / count()
    return "took ${accum().pad(len)} msecs = ${perThing.sigfig(4)} msecs/${thing} (${nthings()} ${thing}s)" +
            " = ${perWhat.sigfig()} msecs/${what} for ${count()} ${what}s"
}

fun Int.pad(len: Int): String = "$this".padStart(len, ' ')
fun Long.pad(len: Int): String = "$this".padStart(len, ' ')

/**
 * Format a double value to have a minimum significant figures.
 *
 * @param minSigfigs minimum significant figures
 * @return double formatted as a string
 */
fun Double.sigfig(minSigfigs: Int = 5): String {
    // fix toString in-equivalence between JS and JVM for X.0 - JVM: X.0, JS: X
    val s: String = if ("$this".matches(Regex("\\d+"))) "${this}.0" else "$this"
    // extract the sign
    val sign: String
    val unsigned: String
    if (s.startsWith("-") || s.startsWith("+")) {
        sign = s.substring(0, 1)
        unsigned = s.substring(1)
    } else {
        sign = ""
        unsigned = s
    }

    // deal with exponential notation
    val mantissa: String
    val exponent: String
    var eInd = unsigned.indexOf('E')
    if (eInd == -1) {
        eInd = unsigned.indexOf('e')
    }
    if (eInd == -1) {
        mantissa = unsigned
        exponent = ""
    } else {
        mantissa = unsigned.substring(0, eInd)
        exponent = unsigned.substring(eInd)
    }

    // deal with decimal point
    var number: StringBuilder
    val fraction: StringBuilder
    val dotInd = mantissa.indexOf('.')
    if (dotInd == -1) {
        number = StringBuilder(mantissa)
        fraction = StringBuilder()
    } else {
        number = StringBuilder(mantissa.substring(0, dotInd))
        fraction = StringBuilder(mantissa.substring(dotInd + 1))
    }

    // number of significant figures
    var numFigs = number.length
    var fracFigs = fraction.length

    // Don't count leading zeros in the fraction, if no number
    if (numFigs == 0 || number.toString() == "0" && fracFigs > 0) {
        numFigs = 0
        number = StringBuilder()
        for (element in fraction) {
            if (element != '0') {
                break
            }
            --fracFigs
        }
    }
    // Don't count trailing zeroes in the number if no fraction
    if (fracFigs == 0 && numFigs > 0) {
        for (i in number.length - 1 downTo 1) {
            if (number[i] != '0') {
                break
            }
            --numFigs
        }
    }
    // deal with min sig figures
    val sigFigs = numFigs + fracFigs
    if (sigFigs > minSigfigs) {
        // Want fewer figures in the fraction; chop (should round? )
        val chop: Int = min(sigFigs - minSigfigs, fracFigs)
        fraction.setLength(fraction.length - chop)
    }

    return if (fraction.isEmpty()) {
        "$sign$number$exponent"
    } else {
        "$sign$number.$fraction$exponent"
    }
}
