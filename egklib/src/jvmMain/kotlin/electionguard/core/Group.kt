package electionguard.core

actual class BigInteger: Comparable<BigInteger> {
    private var value: java.math.BigInteger

    actual companion object {
        actual fun valueOf(value: Long): BigInteger {
            return BigInteger(java.math.BigInteger.valueOf(value))
        }

        actual val ZERO: BigInteger = BigInteger(java.math.BigInteger.ZERO)
        actual val ONE: BigInteger = BigInteger(java.math.BigInteger.ONE)
        actual val TWO: BigInteger = BigInteger(java.math.BigInteger.TWO)
    }

    internal constructor(value: java.math.BigInteger) {
        this.value = value
    }
    actual constructor(value: String) {
        this.value = java.math.BigInteger(value)
    }
    actual constructor(value: String, radix: Int) {
        this.value = java.math.BigInteger(value, radix)
    }
    actual constructor(signum: Int, magnitude: ByteArray) {
        this.value = java.math.BigInteger(signum, magnitude)
    }
    actual constructor(value: ByteArray) {
        this.value = java.math.BigInteger(value)
    }

    actual infix fun shl(n: Int): BigInteger {
        return BigInteger(this.value shl n)
    }

    actual infix fun shr(n: Int): BigInteger {
        return BigInteger(this.value shr n)
    }

    actual infix fun and(other: BigInteger): BigInteger {
        return BigInteger(this.value and other.value)
    }

    actual infix fun or(other: BigInteger): BigInteger {
        return BigInteger(this.value or other.value)
    }

    actual operator fun plus(other: BigInteger): BigInteger {
        return BigInteger(this.value + other.value)
    }

    actual operator fun minus(other: BigInteger): BigInteger {
        return BigInteger(this.value - other.value)
    }

    actual operator fun times(other: BigInteger): BigInteger {
        return BigInteger(this.value * other.value)
    }

    actual operator fun div(other: BigInteger): BigInteger {
        return BigInteger(this.value / other.value)
    }
    actual operator fun rem(m: BigInteger): BigInteger {
        return this.mod(m)
    }

    actual fun pow(exponent: Int): BigInteger {
        return BigInteger(this.value.pow(exponent))
    }
    actual fun modPow(exponent: BigInteger, m: BigInteger): BigInteger {
        return BigInteger(this.value.modPow(exponent.value, m.value))
    }

    actual fun modInverse(m: BigInteger): BigInteger {
        return BigInteger(this.value.modInverse(m.value))
    }

    actual fun mod(m: BigInteger): BigInteger {
        return BigInteger(this.value.mod(m.value))
    }

    actual fun shiftLeft(n: Int): BigInteger {
        return BigInteger(this.value.shiftLeft(n))
    }

    actual override fun compareTo(other: BigInteger): Int {
        return this.value.compareTo(other.value)
    }

    actual override fun equals(other: Any?): Boolean {
        return if (other is BigInteger) {
            this.value == other.value
        } else {
            false
        }
    }

    actual fun toByteArray(): ByteArray {
        return this.value.toByteArray()
    }
}
