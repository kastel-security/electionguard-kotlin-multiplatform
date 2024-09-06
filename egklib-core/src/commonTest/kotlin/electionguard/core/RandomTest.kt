package electionguard.core

import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals

class RandomTest {
    @Test
    fun randomnessIsRandom(): TestResult {
        // we'll go with 128-bit numbers, so coincidences are unlikely
        return runTest {
            val firstBytes = randomBytes(16)

            for (i in 0..100) {
                val moreBytes = randomBytes(16)
                assertEquals(16, moreBytes.size)
                assertContentNotEquals(firstBytes, moreBytes)
            }
        }
    }
}