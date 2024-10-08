package electionguard.cli

import electionguard.cli.RunBatchEncryption.Companion.batchEncryption
import electionguard.core.productionGroup
import electionguard.demonstrate.RandomBallotProvider
import electionguard.publish.makeConsumer
import electionguard.publish.readElectionRecord
import electionguard.testResourcesDir
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.time.Duration.Companion.minutes

class
RunBatchEncryptionTest {
    val nthreads = 25

    @Test
    fun testRunBatchEncryptionWithJsonBallots(): TestResult {
        return runTest(timeout = 5.minutes) {
            RunBatchEncryption.batchEncryptionCli(
                arrayOf(
                    "-in", "$testResourcesDir/workflow/allAvailableJson",
                    "-ballots", "$testResourcesDir/fakeBallots/json",
                    "-out", "testOut/encrypt/testRunBatchEncryptionWithJsonBallots",
                    "-invalid", "testOut/encrypt/testRunBatchEncryptionWithJsonBallots/invalid_ballots",
                    "-nthreads", "$nthreads",
                    "-device", "device2",
                    "--cleanOutput",
                )
            )
            RunVerifier.runVerifier(productionGroup(), "testOut/encrypt/testRunBatchEncryptionWithJsonBallots", 11)
        }
    }

    @Test
    fun testRunBatchEncryptionJson(): TestResult {
        return runTest(timeout = 5.minutes) {
            RunBatchEncryption.batchEncryptionCli(
                arrayOf(
                    "-in", "$testResourcesDir/workflow/allAvailableJson",
                    "-ballots", "$testResourcesDir/fakeBallots/json",
                    "-out", "testOut/encrypt/testRunBatchEncryptionJson",
                    "-invalid", "testOut/encrypt/testRunBatchEncryptionJson/invalid_ballots",
                    "-nthreads", "$nthreads",
                    "-device", "device2",
                    "--cleanOutput",
                    )
            )
            RunVerifier.runVerifier(productionGroup(), "testOut/encrypt/testRunBatchEncryptionJson", 11)
        }
    }

    @Test
    fun testRunBatchEncryptionJsonWithProtoBallots(): TestResult {
        return runTest(timeout = 5.minutes) {
                RunBatchEncryption.batchEncryptionCli(
                    arrayOf(
                        "-in", "$testResourcesDir/workflow/allAvailableJson",
                        "-ballots", "$testResourcesDir/fakeBallots/json",
                        "-out", "testOut/encrypt/testRunBatchEncryptionJsonWithProtoBallots",
                        "-invalid", "testOut/encrypt/testRunBatchEncryptionJsonWithProtoBallots/invalid_ballots",
                        "-nthreads", "$nthreads",
                        "-device", "device3",
                        "--cleanOutput",
                    )
                )
            RunVerifier.runVerifier(productionGroup(), "testOut/encrypt/testRunBatchEncryptionJsonWithProtoBallots", 11)
        }
    }

    @Test
    fun testRunBatchEncryptionEncryptTwice(): TestResult {
        return runTest(timeout = 5.minutes) {
            RunBatchEncryption.batchEncryptionCli(
                arrayOf(
                    "-in", "$testResourcesDir/workflow/allAvailableJson",
                    "-ballots", "$testResourcesDir/fakeBallots/json",
                    "-out", "testOut/encrypt/testRunBatchEncryptionEncryptTwice",
                    "-invalid", "testOut/encrypt/testRunBatchEncryptionEncryptTwice/invalid_ballots",
                    "-nthreads", "$nthreads",
                    "-device", "device4",
                    "-check", "EncryptTwice",
                    "--cleanOutput",
                )
            )
        }
    }

    @Test
    fun testRunBatchEncryptionVerify(): TestResult {
        return runTest(timeout = 5.minutes) {
            RunBatchEncryption.batchEncryptionCli(
                arrayOf(
                    "-in", "$testResourcesDir/workflow/allAvailableJson",
                    "-ballots", "$testResourcesDir/fakeBallots/json",
                    "-out", "testOut/encrypt/testRunBatchEncryptionVerify",
                    "-invalid", "testOut/encrypt/testRunBatchEncryptionVerify/invalid_ballots",
                    "-nthreads", "$nthreads",
                    "-device", "device35",
                    "-check", "Verify",
                    "--cleanOutput",
                )
            )
        }
    }

    @Test
    fun testRunBatchEncryptionVerifyDecrypt(): TestResult {
        return runTest(timeout = 5.minutes) {
            RunBatchEncryption.batchEncryptionCli(
                arrayOf(
                    "-in", "$testResourcesDir/workflow/allAvailableJson",
                    "-ballots", "$testResourcesDir/fakeBallots/json",
                    "-out", "testOut/encrypt/testRunBatchEncryptionVerifyDecrypt",
                    "-invalid", "testOut/encrypt/testRunBatchEncryptionVerifyDecrypt/invalid_ballots",
                    "-nthreads", "$nthreads",
                    "-device", "device42",
                    "-check", "DecryptNonce",
                    "--cleanOutput",
                )
            )
        }
    }

    @Test
    fun testInvalidBallot(): TestResult {
        val inputDir = "$testResourcesDir/workflow/allAvailableJson"
        val outputDir = "testOut/testInvalidBallot"
        val invalidDir = "testOut/testInvalidBallot/invalidDir"
        return runTest(timeout = 5.minutes) {
            val group = productionGroup()
            val electionRecord = readElectionRecord(group, inputDir)

            val ballot = RandomBallotProvider(electionRecord.manifest(), 1).makeBallot()
            val ballots = listOf(ballot.copy(ballotStyle = "badStyleId"))

            batchEncryption(
                group,
                inputDir,
                ballots,
                device = "testDevice",
                outputDir = outputDir,
                encryptDir = null,
                invalidDir = invalidDir,
                1,
                "testInvalidBallot",
            )

            val consumerOut = makeConsumer(group, invalidDir)
            consumerOut.iteratePlaintextBallots(invalidDir, null).forEach {
                println("${it.errors}")
                assertContains(it.errors.toString(), "Ballot.A.1 Ballot Style 'badStyleId' does not exist in election")
            }
        }
    }

}