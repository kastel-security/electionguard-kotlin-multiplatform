package electionguard.cli

import electionguard.cli.RunTrustedBallotDecryption.Companion.runDecryptBallots
import electionguard.cli.RunTrustedTallyDecryption.Companion.readDecryptingTrustees
import electionguard.core.productionGroup
import electionguard.publish.makeConsumer
import electionguard.testResourcesDir
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

/**
 * Test runDecryptBallots with in-process DecryptingTrustee's. Do not use this in production.
 * Note that when the election record changes, the test dataset must be regenerated. For this test, we need to:
 *   1. run showBallotIds() test to see what the possible ballot ids are
 *   2. modify inputDir/private_data/wantedBallots.txt and add 2 valid ballot ids
 *   3. modify testDecryptBallotsSome() and add 3 valid ballot ids to the command line argument
 */
class RunDecryptBallotsTest {
    val nthreads = 25

    @Test
    fun testDecryptBallotsAll(): TestResult {
        return runTest(timeout = 5.minutes) {
            val group = productionGroup()
            val inputDir = "$testResourcesDir/workflow/allAvailableJson"
            val trusteeDir = "$inputDir/private_data/trustees"
            val outputDir = "testOut/decrypt/testDecryptBallotsAll"
            println("\ntestDecryptBallotsAll")
            val n = runDecryptBallots(
                group,
                inputDir,
                outputDir,
                readDecryptingTrustees(group, inputDir, trusteeDir),
                "ALL",
                nthreads,
            )
            assertEquals(11, n)
        }
    }

    @Test
    fun testDecryptBallotsSomeFromList(): TestResult {
        return runTest {
            val group = productionGroup()
            val inputDir = "$testResourcesDir/workflow/someAvailableJson"
            val trusteeDir = "$inputDir/private_data/trustees"
            val outputDir = "testOut/decrypt/testDecryptBallotsSomeFromList"
            println("\ntestDecryptBallotsSomeFromList")
            val n = runDecryptBallots(
                group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir, "5"),
                "id-1," +
                        "id-3," +
                        "id-2",
                3,
            )
            assertEquals(3, n)
        }
    }

    @Test
    fun testDecryptBallotsSomeFromFile(): TestResult {
        return runTest {
            val group = productionGroup()
            val inputDir = "$testResourcesDir/workflow/someAvailableJson"
            val trusteeDir = "$inputDir/private_data/trustees"
            val wantBallots = "$inputDir/private_data/wantedBallots.txt"
            val outputDir = "testOut/decrypt/testDecryptBallotsSomeFromFile"
            println("\ntestDecryptBallotsSomeFromFile")
            val n = runDecryptBallots(
                group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir, "4,5"),
                wantBallots,
                2,
            )
            assertEquals(2, n)
        }
    }

    // decrypt all the ballots
    @Test
    fun testDecryptBallotsMainMultiThreaded(): TestResult {
        println("\ntestDecryptBallotsMainMultiThreaded")
        return runTest(timeout = 5.minutes) {
            RunTrustedBallotDecryption.main(
                arrayOf(
                    "-in",
                    "$testResourcesDir/workflow/someAvailableJson",
                    "-trustees",
                    "$testResourcesDir/workflow/someAvailableJson/private_data/trustees",
                    "-out",
                    "testOut/decrypt/testDecryptBallotsMainMultiThreaded",
                    "-challenged",
                    "all",
                    "-nthreads",
                    "$nthreads"
                )
            )
        }
    }

    // decrypt the ballots marked spoiled
    @Test
    fun testDecryptBallotsMarkedSpoiled(): TestResult {
        println("\ntestDecryptBallotsMainDefault")
        return runTest {
            RunTrustedBallotDecryption.main(
                arrayOf(
                    "-in",
                    "$testResourcesDir/workflow/someAvailableJson",
                    "-trustees",
                    "$testResourcesDir/workflow/someAvailableJson/private_data/trustees",
                    "-out",
                    "testOut/decrypt/testDecryptBallotsMarkedSpoiled",
                    "-nthreads",
                    "1"
                )
            )
        }
    }

    @Test
    fun showBallotIds() {
        val group = productionGroup()
        val inputDir = "$testResourcesDir/workflow/someAvailableJson"
        val ballotDir = "$inputDir/private_data/input/"
        val consumerIn = makeConsumer(group, inputDir)

        consumerIn.iteratePlaintextBallots(ballotDir, null).forEach {
            println(it.ballotId)
        }
    }
}
