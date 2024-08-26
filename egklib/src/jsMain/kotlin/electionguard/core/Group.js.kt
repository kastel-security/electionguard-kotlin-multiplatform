package electionguard.core


actual class BigInteger: Comparable<BigInteger> {
    actual companion object {
        actual fun valueOf(value: Long): BigInteger {
            TODO("Not yet implemented")
        }

        actual val ZERO: BigInteger
            get() = TODO("Not yet implemented")
        actual val ONE: BigInteger
            get() = TODO("Not yet implemented")
        actual val TWO: BigInteger
            get() = TODO("Not yet implemented")
    }

    actual infix fun shl(n: Int): BigInteger {
        TODO("Not yet implemented")
    }

    actual infix fun shr(n: Int): BigInteger {
        TODO("Not yet implemented")
    }

    actual infix fun and(other: BigInteger): BigInteger {
        TODO("Not yet implemented")
    }

    actual infix fun or(other: BigInteger): BigInteger {
        TODO("Not yet implemented")
    }

    actual operator fun plus(other: BigInteger): BigInteger {
        TODO("Not yet implemented")
    }

    actual operator fun minus(other: BigInteger): BigInteger {
        TODO("Not yet implemented")
    }

    actual operator fun times(other: BigInteger): BigInteger {
        TODO("Not yet implemented")
    }

    actual operator fun div(other: BigInteger): BigInteger {
        TODO("Not yet implemented")
    }

    actual fun modPow(
        exponent: BigInteger,
        m: BigInteger
    ): BigInteger {
        TODO("Not yet implemented")
    }

    actual fun modInverse(m: BigInteger): BigInteger {
        TODO("Not yet implemented")
    }

    actual fun mod(m: BigInteger): BigInteger {
        TODO("Not yet implemented")
    }

    actual fun shiftLeft(n: Int): BigInteger {
        TODO("Not yet implemented")
    }

    actual override fun compareTo(other: BigInteger): Int {
        TODO("Not yet implemented")
    }

    actual fun toByteArray(): ByteArray {
        TODO("Not yet implemented")
    }

    actual constructor(value: String) {
        TODO("Not yet implemented")
    }

    actual constructor(signum: Int, magnitude: ByteArray) {
        TODO("Not yet implemented")
    }

    actual constructor(value: String, radix: Int) {
        TODO("Not yet implemented")
    }

}
