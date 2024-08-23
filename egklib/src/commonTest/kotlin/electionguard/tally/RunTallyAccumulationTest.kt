package electionguard.tally

import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedTally
import electionguard.cli.RunAccumulateTally
import electionguard.core.productionGroup
import electionguard.publish.makeConsumer
import kotlin.test.Test
import kotlin.test.assertNotNull

class RunTallyAccumulationTest {

    @Test
    fun runTallyAccumulationTestJson() {
        RunAccumulateTally.main(
            arrayOf(
                "-in",
                "src/commonTest/data/workflow/someAvailableJson",
                "-out",
                "testOut/tally/testRunBatchEncryptionJson",
            )
        )
    }

    @Test
    fun runTallyAccumulationTestProto() {
        RunAccumulateTally.main(
            arrayOf(
                "-in",
                "src/commonTest/data/workflow/someAvailableProto",
                "-out",
                "testOut/tally/runTallyAccumulationTestProto",
                "-name",
                "CountyCook-precinct079-device24358",
                "-createdBy",
                "runTallyAccumulationTestProto",
            )
        )
    }

    @Test
    fun runTallyAccumulationTestJsonNoBallots() {
        val group = productionGroup()
        val consumerIn = makeConsumer(group, "src/commonTest/data/workflow/someAvailableJson")
        val initResult = consumerIn.readElectionInitialized()
        val electionInit = initResult.unwrap()
        val manifest = consumerIn.makeManifest(electionInit.config.manifestBytes)

        val accumulator = AccumulateTally(group, manifest, "name", electionInit.extendedBaseHash, electionInit.jointElGamalPublicKey())
        // nothing accumulated
        val tally: EncryptedTally = accumulator.build()
        assertNotNull(tally)
        /*
        tally.contests.forEach { it.selections.forEach {
            assertEquals( it.encryptedVote ) // show its an encryption of zero - only by decrypting it
        }}
         */
    }
}