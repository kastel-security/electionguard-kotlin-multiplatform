package electionguard.core

import electionguard.core.Base16.toHex
import kotlin.test.Test
import kotlin.test.assertEquals

class BigIntegerCompatibilityTest {

    /** Since java.math.BigInteger uses two's complement with sign extension,
     * and javascript bigint not, we have to test proper conversion*/
    @Test
    fun testBigIntToByteArray() {
        // Positive numbers
        var bi = BigInteger("1")
        assertEquals(byteArrayOf(1).toHex(), bi.toByteArray().toHex())

        bi = BigInteger("127")
        assertEquals(byteArrayOf(0x7F).toHex(), bi.toByteArray().toHex())

        bi = BigInteger("128")
        assertEquals(byteArrayOf(0x00, 0x80.toByte()).toHex(), bi.toByteArray().toHex())

        bi = BigInteger("256")
        assertEquals(byteArrayOf(0x01, 0x00).toHex(), bi.toByteArray().toHex())

        // Negative numbers
        bi = BigInteger("-1")
        assertEquals(byteArrayOf(0xFF.toByte()).toHex(), bi.toByteArray().toHex())

        bi = BigInteger("-128")
        assertEquals(byteArrayOf(0x80.toByte()).toHex(), bi.toByteArray().toHex())

        bi = BigInteger("-129")
        assertEquals(byteArrayOf(0xFF.toByte(), 0x7F).toHex(), bi.toByteArray().toHex())

        bi = BigInteger("-256")
        assertEquals(byteArrayOf(0xFF.toByte(), 0x00).toHex(), bi.toByteArray().toHex())

        // Zero
        bi = BigInteger("0")
        assertEquals(byteArrayOf(0x00).toHex(), bi.toByteArray().toHex())
    }
}
