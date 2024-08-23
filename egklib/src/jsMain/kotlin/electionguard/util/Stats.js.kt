package electionguard.util

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