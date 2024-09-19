package electionguard.core

import electionguard.core.Base16.toHex
import kotlin.experimental.inv

@JsModule(import = "big-integer")
@JsNonModule
external class BigInt {
    class ArrayObject {
        val value: Array<Number>
        val isNegative: Boolean
    }
    constructor(value: Any)
    constructor(value: Any, radix: Number)
    fun add(other: BigInt): BigInt
    fun minus(other: BigInt): BigInt
    fun multiply(other: BigInt): BigInt
    fun divide(other: BigInt): BigInt
    fun shiftLeft(n: Number): BigInt
    fun shiftRight(n: Number): BigInt
    fun and(other: BigInt): BigInt
    fun or(other: BigInt): BigInt
    fun mod(m: BigInt): BigInt
    fun modInv(m: BigInt): BigInt
    fun modPow(exp: BigInt, m: BigInt): BigInt
    fun compare(other: BigInt): Number
    fun toArray(radix: Number): ArrayObject
    fun pow(exp: Number): BigInt
    fun toString(radix: Number = definedExternally): String
}

actual class BigInteger: Comparable<BigInteger> {
    private var value: BigInt
    actual companion object {
        actual fun valueOf(value: Long): BigInteger {
            return BigInteger(value.toString(10))
        }

        actual val ZERO: BigInteger = BigInteger("0")
        actual val ONE: BigInteger = BigInteger("1")
        actual val TWO: BigInteger = BigInteger("2")
    }

    actual infix fun shl(n: Int): BigInteger {
        return this.shiftLeft(n)
    }

    actual infix fun shr(n: Int): BigInteger {
        return BigInteger(this.value.shiftRight(n))
    }

    actual infix fun and(other: BigInteger): BigInteger {
        return BigInteger(this.value.and(other.value))
    }

    actual infix fun or(other: BigInteger): BigInteger {
        return BigInteger(this.value.or(other.value))
    }

    actual operator fun plus(other: BigInteger): BigInteger {
        return BigInteger(this.value.add(other.value).toString())
    }

    actual operator fun minus(other: BigInteger): BigInteger {
        return BigInteger(this.value.minus(other.value).toString())
    }

    actual operator fun times(other: BigInteger): BigInteger {
        return BigInteger(this.value.multiply(other.value).toString())
    }

    actual operator fun div(other: BigInteger): BigInteger {
        return BigInteger(this.value.divide(other.value).toString())
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
        return BigInteger(this.value.modInv(m.value))
    }

    actual fun mod(m: BigInteger): BigInteger {
        return BigInteger(this.value.mod(m.value))
    }

    actual fun shiftLeft(n: Int): BigInteger {
        return BigInteger(this.value.shiftLeft(n))
    }

    actual override fun compareTo(other: BigInteger): Int {
        return this.value.compare(other.value).toInt()
    }

    actual fun toByteArray(): ByteArray {
        val array = this.value.toArray(256)
        val isNegative = array.isNegative
        val bytes = array.value.map { it.toByte() }.toMutableList()

        if (isNegative) {
            // Two's complement for negative numbers
            var carry = true
            for (i in bytes.indices.reversed()) {
                bytes[i] = bytes[i].inv()
                if (carry) {
                    if (bytes[i] == 0xFF.toByte()) {
                        bytes[i] = 0
                    } else {
                        bytes[i] = (bytes[i] + 1).toByte()
                        carry = false
                    }
                }
            }
            // Ensure sign extension for negative numbers
            if (bytes[0] >= 0) {
                bytes.add(0, 0xFF.toByte()) // Add an extra byte if MSB doesn't correctly represent the sign
            }
        }

        if (bytes[0] < 0 && !isNegative) {
            bytes.add(0, 0) // positive sign extension
        }
        return bytes.toByteArray()
    }

    actual override fun toString(): String {
        return this.value.toString()
    }

     actual override fun equals(other: Any?): Boolean {
         return if (other is BigInteger) {
             this.value == other.value
         } else {
             false
         }
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    private constructor(value: BigInt) {
        this.value = value
    }

    actual constructor(value: String) {
        this.value = BigInt(value)
    }

    actual constructor(signum: Int, magnitude: ByteArray) {
        when (signum) {
            1 -> this.value = BigInt(magnitude.toHex(), 16)
            -1 -> this.value = BigInt(magnitude.toHex(), 16).multiply(BigInt(-1))
            else -> throw IllegalArgumentException("Illegal signum")
        }
    }

    actual constructor(value: String, radix: Int) {
        this.value = BigInt(value, radix)
    }
    actual constructor(value: ByteArray) {
        this.value = BigInt(value.toHex(), 16)
    }

}
