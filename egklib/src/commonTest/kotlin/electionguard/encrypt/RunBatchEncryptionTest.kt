package electionguard.encrypt

import electionguard.core.productionGroup
import electionguard.input.RandomBallotProvider
import electionguard.publish.makeConsumer
import electionguard.publish.readElectionRecord
import kotlin.test.Test
import kotlin.test.assertContains

class RunBatchEncryptionTest {
    val nthreads = 25

    @Test
    fun testRunBatchEncryptionProto() {
        main(
            arrayOf(
                "-in",
                "testOut/keyceremony/testKeyCeremonyProto",
                "-ballots",
                "testOut/fakeBallots/proto",
                "-out",
                "testOut/encrypt/testRunBatchEncryptionProto",
                "-invalid",
                "testOut/encrypt/testRunBatchEncryptionProto/invalid_ballots",
                "-nthreads",
                "$nthreads",
            )
        )
    }

    @Test
    fun testRunBatchEncryptionWithJsonBallots() {
        main(
            arrayOf(
                "-in",
                "testOut/keyceremony/testKeyCeremonyProto",
                "-ballots",
                "testOut/fakeBallots/json",
                "-out",
                "testOut/encrypt/testRunBatchEncryptionWithJsonBallots",
                "-invalid",
                "testOut/encrypt/testRunBatchEncryptionWithJsonBallots/invalid_ballots",
                "-nthreads",
                "$nthreads",
            )
        )
    }


    @Test
    fun testRunBatchEncryptionJson() {
        main(
            arrayOf(
                "-in",
                "testOut/keyceremony/testKeyCeremonyJson",
                "-ballots",
                "testOut/fakeBallots/json",
                "-out",
                "testOut/encrypt/testRunBatchEncryptionJson",
                "-invalid",
                "testOut/encrypt/testRunBatchEncryptionJson/invalid_ballots",
                "-nthreads",
                "$nthreads",
            )
        )
    }

    @Test
    fun testRunBatchEncryptionJsonWithProtoBallots() {
        main(
            arrayOf(
                "-in",
                "testOut/keyceremony/testKeyCeremonyJson",
                "-ballots",
                "testOut/fakeBallots/proto",
                "-out",
                "testOut/encrypt/testRunBatchEncryptionJsonWithProtoBallots",
                "-invalid",
                "testOut/encrypt/testRunBatchEncryptionJsonWithProtoBallots/invalid_ballots",
                "-nthreads",
                "$nthreads",
            )
        )
    }

    @Test
    fun testRunBatchEncryptionChain() {
        main(
            arrayOf(
                "-in",
                "testOut/keyceremony/testKeyCeremonyJson",
                "-ballots",
                "testOut/fakeBallots/json",
                "-out",
                "testOut/encrypt/testRunBatchEncryptionChain",
                "-invalid",
                "testOut/encrypt/testRunBatchEncryptionChain/invalid_ballots",
                "-nthreads",
                "$nthreads",
                "-chainCodes",
            )
        )
    }

    @Test
    fun testRunBatchEncryptionEncryptTwice() {
        main(
            arrayOf(
                "-in",
                "testOut/keyceremony/testKeyCeremonyJson",
                "-ballots",
                "testOut/fakeBallots/json",
                "-out",
                "testOut/encrypt/testRunBatchEncryptionEncryptTwice",
                "-invalid",
                "testOut/encrypt/testRunBatchEncryptionEncryptTwice/invalid_ballots",
                "-nthreads",
                "$nthreads",
                "-check",
                "EncryptTwice"
            )
        )
    }

    @Test
    fun testRunBatchEncryptionVerify() {
        main(
            arrayOf(
                "-in",
                "testOut/keyceremony/testKeyCeremonyJson",
                "-ballots",
                "testOut/fakeBallots/json",
                "-out",
                "testOut/encrypt/testRunBatchEncryptionVerify",
                "-invalid",
                "testOut/encrypt/testRunBatchEncryptionVerify/invalid_ballots",
                "-nthreads",
                "$nthreads",
                "-check",
                "Verify",
            )
        )
    }

    @Test
    fun testRunBatchEncryptionVerifyDecrypt() {
        main(
            arrayOf(
                "-in",
                "testOut/keyceremony/testKeyCeremonyJson",
                "-ballots",
                "testOut/fakeBallots/json",
                "-out",
                "testOut/encrypt/testRunBatchEncryptionVerifyDecrypt",
                "-invalid",
                "testOut/encrypt/testRunBatchEncryptionVerifyDecrypt/invalid_ballots",
                "-nthreads",
                "$nthreads",
                "-check",
                "DecryptNonce",
            )
        )
    }

    @Test
    fun testInvalidBallot() {
        val inputDir = "testOut/keyceremony/testKeyCeremonyProto"
        val invalidDir = "testOut/testInvalidBallot"

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, inputDir)
        val ballots = RandomBallotProvider(electionRecord.manifest(), 1).ballots("badStyleId")

        batchEncryption(
            group,
            inputDir,
            invalidDir,
            ballots,
            invalidDir,
            false,
            1,
            "testInvalidBallot",
        )

        val consumerOut = makeConsumer(invalidDir, group)
        consumerOut.iteratePlaintextBallots(invalidDir, null).forEach {
            println("${it.errors}")
            assertContains(it.errors.toString(), "Ballot.A.1 Ballot Style 'badStyleId' does not exist in election")
        }

    }

}