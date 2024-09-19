package electionguard.util

import electionguard.testResourcesDir
import kotlin.test.Test
import kotlin.test.assertTrue

class UtilsKommTest {
    private val testData = "$testResourcesDir/fakeBallots"

    @Test
    fun accessResources() {
        assertTrue { pathExists(testData) }
    }
}
