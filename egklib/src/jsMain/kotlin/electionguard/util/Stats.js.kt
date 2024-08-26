package electionguard.util

actual class Stats {
    actual fun of(who: String, thing: String, what: String): Stat {
        TODO("Not yet implemented")
    }

    actual fun show(who: String) {
    }

    actual fun show(len: Int) {
    }

    actual fun get(who: String): Stat? {
        TODO("Not yet implemented")
    }

    actual fun count(): Int {
        TODO("Not yet implemented")
    }

    actual fun showLines(len: Int): List<String> {
        TODO("Not yet implemented")
    }
}

actual class Stat actual constructor(thing: String, what: String) {
    actual fun thing(): String {
        TODO("Not yet implemented")
    }

    actual fun what(): String {
        TODO("Not yet implemented")
    }

    actual fun accum(amount: Long, nthings: Int) {
    }

    actual fun accum(): Long {
        TODO("Not yet implemented")
    }

    actual fun nthings(): Int {
        TODO("Not yet implemented")
    }

    actual fun count(): Int {
        TODO("Not yet implemented")
    }

    actual fun copy(accum: Long): Stat {
        TODO("Not yet implemented")
    }
}