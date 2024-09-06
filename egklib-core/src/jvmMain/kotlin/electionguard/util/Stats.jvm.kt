package electionguard.util

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

actual class Stats {
    private val mutex = Mutex()
    private val stats = mutableMapOf<String, Stat>() // TODO need thread safe collection

    actual fun of(who: String, thing: String, what: String): Stat {
        return runBlocking {
            mutex.withLock {
                stats.getOrPut(who) { Stat(thing, what) }
            }
        }
    }

    actual fun show(who: String) {
        val stat = stats.get(who)
        if (stat != null) println(stat.show()) else println("no stat named $who")
    }

    actual fun get(who: String) : Stat? = stats.get(who)

    actual fun show(len: Int) {
        showLines(len).forEach { println(it) }
    }

    actual fun count() : Int {
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

/** So we can use AtomicXXX */
actual class Stat actual constructor(
    val thing : String,
    val what: String
) {
    var accum : AtomicLong = AtomicLong(0)
    var count : AtomicInteger = AtomicInteger(0)
    var nthings : AtomicInteger = AtomicInteger(0)

    actual fun accum(amount : Long, nthings : Int) {
        accum.addAndGet(amount)
        this.nthings.addAndGet(nthings)
        count.incrementAndGet()
    }

    actual fun copy(accum: Long): Stat {
        val copy = Stat(this.thing, this.what)
        copy.accum = AtomicLong(accum)
        copy.count = this.count
        copy.nthings = this.nthings
        return copy
    }

    actual fun thing() = this.thing

    actual fun what() = this.what

    actual fun accum() = this.accum.get()

    actual fun nthings() = this.nthings.get()

    actual fun count() = this.count.get()
}
