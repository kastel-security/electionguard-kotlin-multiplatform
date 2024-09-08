package electionguard.util

/**
 * The javascript implementation of STats is not thread-safe
 */
actual class Stats {
    private val stats = mutableMapOf<String, Stat>()
    actual fun of(who: String, thing: String, what: String): Stat =
        stats.getOrPut(who) { Stat(thing, what) }

    actual fun show(who: String) {
        val stat = stats[who]
        if (stat != null) println(stat.show()) else println("no stat named $who")
    }

    actual fun show(len: Int) = showLines(len).forEach { println(it) }

    actual fun get(who: String): Stat? = stats[who]

    actual fun count(): Int = if (stats.isNotEmpty()) stats.values.first().count() else 0

    actual fun showLines(len: Int): List<String> {
        val result = mutableListOf<String>()
        if (stats.isEmpty()) {
            result.add("stats is empty")
            return result
        }
        var accum = 0L
        var nThings = 0
        var count = 0
        stats.forEach {
            result.add("${it.key.padStart(20, ' ')}: ${it.value.show(len)}")
            accum += it.value.accum()
            nThings += it.value.nthings()
            count += it.value.count()
        }
        val total = stats.values.first().copy(accum, nThings, count)
        val totalName = "total".padStart(20, ' ')
        result.add("$totalName: ${total.show(len)}")
        return result
    }
}

actual class Stat actual constructor(
    actual val thing: String,
    actual val what: String
) {
    private var accum: Long = 0
    private var count: Int = 0
    private var nthings: Int = 0

    actual fun accum(): Long = accum
    actual fun nthings(): Int = nthings
    actual fun count(): Int = count

    actual fun accum(amount: Long, nthings: Int) {
        accum += amount
        this.nthings += nthings
        count += 1
    }

    actual fun copy(accum: Long, nthings: Int, count: Int): Stat {
        val copy = Stat(this.thing, this.what)
        copy.accum = accum
        copy.count = count
        copy.nthings = nthings
        return copy
    }
}
