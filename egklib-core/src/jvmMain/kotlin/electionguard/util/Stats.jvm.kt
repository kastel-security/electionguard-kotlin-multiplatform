package electionguard.util

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

actual class Stats {
    private val mutex = Mutex()
    private val stats = ConcurrentHashMap<String, Stat>()

    actual fun of(who: String, thing: String, what: String): Stat {
        return runBlocking {
            mutex.withLock {
                stats.getOrPut(who) { Stat(thing, what) }
            }
        }
    }

    actual fun show(who: String) {
        val stat = stats[who]
        if (stat != null) println(stat.show()) else println("no stat named $who")
    }

    actual fun get(who: String): Stat? = stats[who]

    actual fun show(len: Int) {
        showLines(len).forEach { println(it) }
    }

    actual fun count(): Int {
        return if (stats.isNotEmpty()) stats.values.first().count() else 0
    }

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

/** So we can use AtomicXXX */
actual class Stat actual constructor(
    actual val thing: String,
    actual val what: String
) {
    private var atomicAccum: AtomicLong = AtomicLong(0)
    private var atomicCount: AtomicInteger = AtomicInteger(0)
    private var atomicNThings: AtomicInteger = AtomicInteger(0)

    actual fun accum(amount: Long, nthings: Int) {
        atomicAccum.addAndGet(amount)
        atomicNThings.addAndGet(nthings)
        atomicCount.incrementAndGet()
    }

    actual fun copy(accum: Long, nthings: Int, count: Int): Stat {
        val copy = Stat(this.thing, this.what)
        copy.atomicAccum = AtomicLong(accum)
        copy.atomicCount = AtomicInteger(count)
        copy.atomicNThings = AtomicInteger(nthings)
        return copy
    }

    actual fun accum() = this.atomicAccum.get()

    actual fun nthings() = this.atomicNThings.get()

    actual fun count() = this.atomicCount.get()
}
