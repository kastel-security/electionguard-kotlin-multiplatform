package electionguard.encrypt

import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.core.*
import electionguard.input.RandomBallotProvider
import electionguard.publish.readElectionRecord
import electionguard.util.ErrorMessages
import kotlin.math.roundToInt
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Verify the embedded nonces in an Encrypted Ballot. */
@Ignore // I/O is not supported in browser tests
class EncryptionNonceTest {
    val input = "src/commonTest/data/workflow/allAvailableJson"
    val nballots = 11

    @Test
    fun testEncryptionNonces() {
        val group = productionGroup()
        val electionRecord = readElectionRecord(group, input)
        val electionInit = electionRecord.electionInit()!!

        // encrypt
        val encryptor = Encryptor(
            group,
            electionRecord.manifest(),
            ElGamalPublicKey(electionInit.jointPublicKey),
            electionInit.extendedBaseHash,
            "device"
        )

        val starting = getSystemTimeInMillis()
        RandomBallotProvider(electionRecord.manifest(), nballots).ballots().forEach { ballot ->
            val ciphertextBallot = encryptor.encrypt(ballot, ByteArray(0), ErrorMessages("testEncryptionNonces"))!!
            // decrypt with nonces
            val decryptionWithNonce = VerifyEmbeddedNonces(group, electionRecord.manifest(), electionInit.jointElGamalPublicKey(), electionInit.extendedBaseHash)
            val decryptedBallot = with (decryptionWithNonce) { ciphertextBallot.decrypt() }
            assertNotNull(decryptedBallot)

            compareBallots(ballot, decryptedBallot)
        }

        val took = getSystemTimeInMillis() - starting
        val msecsPerBallot = (took.toDouble() / nballots).roundToInt()
        println("testEncryptionNonces $nballots took $took millisecs for $nballots ballots = $msecsPerBallot msecs/ballot")
    }
}

fun compareBallots(ballot: PlaintextBallot, decryptedBallot: PlaintextBallot) {
    assertEquals(ballot.ballotId, decryptedBallot.ballotId)
    assertEquals(ballot.ballotStyle, decryptedBallot.ballotStyle)

    // all non zero votes match
    ballot.contests.forEach { contest1 ->
        val contest2 = decryptedBallot.contests.find { it.contestId == contest1.contestId }
        assertNotNull(contest2)
        contest1.selections.forEach { selection1 ->
            val selection2 = contest2.selections.find { it.selectionId == selection1.selectionId }
            assertNotNull(selection2)
            assertEquals(selection1, selection2)
        }
    }

    // all votes match
    decryptedBallot.contests.forEach { contest2 ->
        val contest1 = decryptedBallot.contests.find { it.contestId == contest2.contestId }
        if (contest1 == null) {
            contest2.selections.forEach { assertEquals(it.vote, 0) }
        } else {
            contest2.selections.forEach { selection2 ->
                val selection1 = contest1.selections.find { it.selectionId == selection2.selectionId }
                if (selection1 == null) {
                    assertEquals(selection2.vote, 0)
                } else {
                    assertEquals(selection1, selection2)
                }
            }
        }
    }
}

class VerifyEmbeddedNonces(val group : GroupContext, val manifest: Manifest, val publicKey: ElGamalPublicKey, val extendedBaseHash: UInt256) {

    fun CiphertextBallot.decrypt(): PlaintextBallot {
        val ballotNonce: UInt256 = this.ballotNonce

        val plaintext_contests = mutableListOf<PlaintextBallot.Contest>()
        for (contest in this.contests) {
            val plaintextContest = verifyContestNonces(ballotNonce, contest)
            assertNotNull(plaintextContest)
            plaintext_contests.add(plaintextContest)
        }
        return PlaintextBallot(
            this.ballotId,
            this.ballotStyleId,
            plaintext_contests,
            null
        )
    }

    private fun verifyContestNonces(
        ballotNonce: UInt256,
        contest: CiphertextBallot.Contest
    ): PlaintextBallot.Contest {

        val plaintextSelections = mutableListOf<PlaintextBallot.Selection>()
        for (selection in contest.selections) {
            val plaintextSelection = verifySelectionNonces(ballotNonce, contest.sequenceOrder, selection)
            assertNotNull(plaintextSelection)
            plaintextSelections.add(plaintextSelection)
        }
        return PlaintextBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            plaintextSelections
        )
    }

    private fun verifySelectionNonces(
        ballotNonce: UInt256,
        contestIndex: Int,
        selection: CiphertextBallot.Selection
    ): PlaintextBallot.Selection? {
        val selectionNonce = hashFunction(extendedBaseHash.bytes, 0x20.toByte(), ballotNonce, contestIndex, selection.sequenceOrder).toElementModQ(group) // eq 25
        assertEquals(selectionNonce, selection.selectionNonce)

        val decodedVote: Int? = selection.ciphertext.decryptWithNonce(publicKey, selection.selectionNonce)
        return decodedVote?.let {
            PlaintextBallot.Selection(
                selection.selectionId,
                selection.sequenceOrder,
                decodedVote,
            )
        }
    }
}