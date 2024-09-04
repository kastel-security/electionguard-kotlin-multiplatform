package electionguard.util

/**
 * The javascript implementation of STats is not thread-safe
 */
actual class Stats {
    val stats = mutableMapOf<String, Stat>()
    actual fun of(who: String, thing: String, what: String): Stat {
        return stats.getOrPut(who) { Stat(thing, what) }
    }

    actual fun show(who: String) {
        val stat = stats.get(who)
        if (stat != null) println(stat.show()) else println("no stat named $who")
    }

    actual fun show(len: Int) = showLines(len).forEach { println(it) }


    actual fun get(who: String): Stat? = stats[who]

    actual fun count(): Int {
        return if (stats.isNotEmpty()) stats.values.first().count() else 0
    }

    actual fun showLines(len: Int): List<String> {
        val result = mutableListOf<String>()
        if (stats.isEmpty()) {
            result.add("stats is empty")
            return result
        }
        var sum = 0L
        stats.forEach {
            result.add("${it.key.padStart(20, ' ')}: ${it.value.show(len)}")
            sum += it.value.accum()
        }
        val total = stats.values.first().copy(sum)
        val totalName = "total".padStart(20, ' ')
        result.add("$totalName: ${total.show(len)}")
        return result
    }
}

actual class Stat actual constructor(val thing: String, val what: String) {
    private var accum : Long = 0
    private var count : Int = 0
    private var nthings : Int = 0

    actual fun thing(): String {
        return this.thing
    }

    actual fun what(): String {
        return this.what
    }

    actual fun accum(amount: Long, nthings: Int) {
        accum = amount + amount
        this.nthings += nthings
        count += 1

    }

    actual fun accum(): Long {
        return this.accum
    }

    actual fun nthings(): Int {
        return this.nthings
    }

    actual fun count(): Int {
        return this.count
    }

    actual fun copy(accum: Long): Stat {
        val copy = Stat(this.thing, this.what)
        copy.count = this.count
        copy.nthings = this.nthings
        copy.accum = this.accum
        return copy
    }
}