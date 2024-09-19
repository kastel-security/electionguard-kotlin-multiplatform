package electionguard.json

import electionguard.core.ChaumPedersenProof
import electionguard.core.elementsModQ
import electionguard.core.productionGroup
import electionguard.jsonRoundTrip
import electionguard.runTest
import io.kotest.property.checkAll
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals

class ChaumPedersenJsonTest {
    @Test
    fun testRoundtrip(): TestResult {
        return runTest {
            val group = productionGroup()
            checkAll(
                iterations = 33,
                elementsModQ(group),
                elementsModQ(group),
            ) { challenge, response ->
                val goodProof = ChaumPedersenProof(challenge, response)
                assertEquals(goodProof, goodProof.publishJson().import(group))
                assertEquals(goodProof, jsonRoundTrip(goodProof.publishJson()).import(group))
            }
        }
    }
}
