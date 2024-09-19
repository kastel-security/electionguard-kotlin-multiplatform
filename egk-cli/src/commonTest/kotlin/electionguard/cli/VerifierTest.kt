package electionguard.cli

import electionguard.core.productionGroup
import electionguard.testResourcesDir
import kotlin.test.Test

class VerifierTest {
    @Test
    fun verifyRemoteWorkflow() {
        try {
            RunVerifier.runVerifier(
                productionGroup(),
                "$testResourcesDir/testElectionRecord/remoteWorkflow/keyceremony",
                11
            )
            RunVerifier.runVerifier(
                productionGroup(),
                "$testResourcesDir/testElectionRecord/remoteWorkflow/electionRecord",
                11
            )
        } catch (t :Throwable) {
            t.printStackTrace()
        }
        // RunVerifier.runVerifier(productionGroup(), "/home/stormy/dev/github/egk-webapps/testOut/remoteWorkflow/keyceremony/", 11)
        // RunVerifier.runVerifier(productionGroup(), "/home/stormy/dev/github/egk-webapps/testOut/remoteWorkflow/electionRecord/", 11)
    }

    @Test
    fun verificationAllJson() {
        RunVerifier.runVerifier(productionGroup(), "$testResourcesDir/workflow/allAvailableJson", 11, true)
    }

    @Test
    fun verificationSomeJson() {
        RunVerifier.runVerifier(productionGroup(), "$testResourcesDir/workflow/someAvailableJson", 11, true)
    }

    // @Test
    fun testProblem() {
        RunVerifier.runVerifier(productionGroup(), "../testOut/cliWorkflow/electionRecord", 11, true)
    }

    @Test
    fun readRecoveredElectionRecordAndValidate() {
        RunVerifier.main(
            arrayOf(
                "-in",
                "$testResourcesDir/workflow/someAvailableJson",
                "-nthreads",
                "11",
                "--showTime",
            )
        )
    }

    @Test
    fun testVerifyEncryptedBallots() {
        RunVerifier.verifyEncryptedBallots(productionGroup(), "$testResourcesDir/workflow/someAvailableJson", 11)
    }

    @Test
    fun verifyDecryptedTallyWithRecoveredShares() {
        RunVerifier.verifyDecryptedTally(productionGroup(), "$testResourcesDir/workflow/someAvailableJson")
    }

    @Test
    fun verifySpoiledBallotTallies() {
        RunVerifier.verifyChallengedBallots(productionGroup(), "$testResourcesDir/workflow/chainedJson")
    }

    // Ordered lists of the ballots encrypted by each device. spec 2.0, section 3.7, p.46
    @Test
    fun testVerifyTallyBallotIds() {
        RunVerifier.verifyTallyBallotIds(productionGroup(), "$testResourcesDir/workflow/allAvailableJson")
        RunVerifier.verifyTallyBallotIds(productionGroup(), "$testResourcesDir/workflow/someAvailableJson")
    }
}
