package electionguard.core

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformRandomTest {
    @Test
    fun testRandomBytes() {
        val randomBytes = randomBytes(32)
        assertTrue { randomBytes.size == 32 }
    }
}
