package electionguard.workflow

import electionguard.model.DecryptedTallyOrBallot
import electionguard.cli.RunAccumulateTally.Companion.runAccumulateBallots
import electionguard.cli.RunBatchEncryption.Companion.batchEncryption
import electionguard.cli.RunCreateElectionConfig
import electionguard.cli.RunTrustedBallotDecryption
import electionguard.cli.RunTrustedTallyDecryption.Companion.runDecryptTally
import electionguard.core.*
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.publish.*
import electionguard.testResourcesDir
import electionguard.util.Stats
import electionguard.verifier.Verifier
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

/**
 * Run workflow with varying number of guardians, on the same ballots, and compare the results.
 * Also show operation Counts.
 */
class TestNumGuardians {
    val group = productionGroup()

    private val manifestJson = "$testResourcesDir/startManifestJson/manifest.json"
    private val inputBallotDir = "$testResourcesDir/fakeBallots/json"
    val name1 = "runWorkflowOneGuardian"
    val name2 = "runWorkflowThreeGuardian"
    val name3 = "runWorkflow5of6Guardian"
    val name4 = "runWorkflow8of10Guardian"

    @Test
    fun runWorkflows() {
        println("productionGroup (Default) = $group class = ${group::class.toString()}")
        //runWorkflow(name1, 1, 1, listOf(1), 1)
        runWorkflow(name1, 1, 1, listOf(1), 25)

        //runWorkflow(name2, 3, 3, listOf(1,2,3), 1)
        runWorkflow(name2, 3, 3, listOf(1,2,3), 25)

        // runWorkflow(name3, 6, 5, listOf(1,2,4,5,6), 1)
        runWorkflow(name3, 6, 5, listOf(1,2,4,5,6), 25)

        //runWorkflow(name4, 10, 8, listOf(1,2,4,5,6,7,8,9), 1)
        runWorkflow(name4, 10, 8, listOf(1,2,4,5,6,7,8,9), 25)

        checkTalliesAreEqual()
        checkBallotsAreEqual()
    }

    fun runWorkflow(name : String, nguardians: Int, quorum: Int, present: List<Int>, nthreads: Int): TestResult {
        return runTest(timeout = 5.minutes) {
            println("===========================================================")
            val workingDir = "testOut/workflow/$name"
            val privateDir = "$workingDir/private_data"
            val trusteeDir = "${privateDir}/trustees"
            val invalidDir = "${privateDir}/invalid"

            // delete current workingDir
            makePublisher(workingDir, true)

            RunCreateElectionConfig.main(
                arrayOf(
                    "-manifest", manifestJson,
                    "-nguardians", nguardians.toString(),
                    "-quorum", quorum.toString(),
                    "-out", workingDir,
                    "-device", "device11",
                    "-createdBy", name1,
                )
            )

            // key ceremony
            val (_, init) = runFakeKeyCeremony(group, workingDir, workingDir, trusteeDir, nguardians, quorum, false)
            println("FakeKeyCeremony created ElectionInitialized, guardians = $present")

            // encrypt
            batchEncryption(
                group, inputDir = workingDir, ballotDir = inputBallotDir, device = "device11",
                outputDir = workingDir, null, invalidDir = invalidDir, nthreads, name1
            )

            // tally
            runAccumulateBallots(group, workingDir, workingDir, null, "RunWorkflow", name1)

            val dtrustees: List<DecryptingTrusteeIF> = readDecryptingTrustees(group, trusteeDir, init, present, true)
            runDecryptTally(group, workingDir, workingDir, dtrustees, name1)

            // decrypt ballots
            RunTrustedBallotDecryption.trustedBallotDecryptionCli(
                arrayOf(
                    "-in", workingDir,
                    "-trustees", trusteeDir,
                    "-out", workingDir,
                    "-challenged", "all",
                    "-nthreads", nthreads.toString()
                )
            )

            // verify
            println("\nRun Verifier")
            val record = readElectionRecord(group, workingDir)
            val verifier = Verifier(record, nthreads)
            val stats = Stats()
            val ok = verifier.verify(stats)
            stats.show()
            println("Verify is $ok")
            assertTrue(ok)
        }
    }

    fun checkTalliesAreEqual() {
        val record1 =  readElectionRecord(group, "testOut/workflow/$name1")
        val record2 =  readElectionRecord(group, "testOut/workflow/$name2")
        testEqualTallies(record1.decryptedTally()!!, record2.decryptedTally()!!)

        val record3 =  readElectionRecord(group, "testOut/workflow/$name3")
        testEqualTallies(record1.decryptedTally()!!, record3.decryptedTally()!!)
        testEqualTallies(record2.decryptedTally()!!, record3.decryptedTally()!!)

        val record4 =  readElectionRecord(group, "testOut/workflow/$name4")
        testEqualTallies(record1.decryptedTally()!!, record4.decryptedTally()!!)
        testEqualTallies(record2.decryptedTally()!!, record4.decryptedTally()!!)
    }

    fun checkBallotsAreEqual() {
        val record1 =  readElectionRecord(group, "testOut/workflow/$name1")
        val record2 =  readElectionRecord(group, "testOut/workflow/$name2")
        println("compare ${record1.topdir()} ${record2.topdir()}")

        val ballotsa = record1.decryptedBallots().iterator()
        val ballotsb = record2.decryptedBallots().iterator()
        while (ballotsa.hasNext()) {
            testEqualTallies(ballotsa.next(), ballotsb.next())
        }
    }

    fun testEqualTallies(tallya : DecryptedTallyOrBallot, tallyb : DecryptedTallyOrBallot) {
        assertEquals(tallya.id, tallyb.id)
        print(" compare ${tallya.id} ${tallyb.id}")
        tallya.contests.zip(tallyb.contests).forEach { (contesta, contestb) ->
            assertEquals(contesta.contestId, contestb.contestId)
            contesta.selections.zip(contestb.selections).forEach { (selectiona, selectionb) ->
                assertEquals(selectiona.selectionId, selectionb.selectionId)
                assertEquals(selectiona.tally, selectionb.tally)
                print(" OK")
            }
        }
        println()
    }
}