package electionguard

import io.mockk.InternalPlatformDsl.toStr
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.math.BigInteger

class BigIntTest {
    @Test
    fun testBigInteger() {
        val b1 = BigInteger("1EBC1E9BCC5FB0D9AC8E88F3914483403929656924518C36C596AB14A1BE8A9E", 16)
        val b2 = BigInteger("7BE7222D305B814AC0FDA6DE3E4A352D66BC41CF4BEFB166436B63A66D82418D", 16)
        val b3 = BigInteger("1a17149f0a34d268c22aca4c28a49d4891", 16)

        println(Json.encodeToString(b1.toByteArray()))
        println((b1 + b2).toString())
        println((b1 - b2).toString())
        println((b1 * b2).toString())
        println((b2 / b1).toString())
        println((b1 shl 3).toString())
        println((b1 shr 3).toString())
        println(b1.and(b2).toString())
        println(b1.or(b2).toString())
        println(b1.mod(b3).toString())
        println(b1.modInverse(b3).toString())
        println(b1.modPow(b2, b3).toString())

    }
}