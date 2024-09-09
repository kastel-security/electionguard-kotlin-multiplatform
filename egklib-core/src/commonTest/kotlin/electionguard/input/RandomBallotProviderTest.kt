package electionguard.input

import electionguard.demonstrate.RandomBallotProvider
import electionguard.demonstrate.buildTestManifest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class RandomBallotProviderTest {

    @Test
    fun testBadStyle() {
        val manifest = buildTestManifest(3, 4)

        val exception = assertFailsWith<RuntimeException>(
            block = { RandomBallotProvider(manifest, 1).ballots("badStyleId") }
        )
        assertEquals(
            "BallotStyle 'badStyleId' not found in manifest ballotStyles= ['ballotStyle': [district9]]",
            exception.message
        )
    }

}