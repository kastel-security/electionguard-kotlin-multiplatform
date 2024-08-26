import electionguard.core.BigInteger
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test

class BigIntegerTest {
    @Test
    fun testBasicOperations() {
        val i1 = BigInteger("123")
        val i2 = BigInteger("123")
        println((i1 + i2).toString())
    }
}