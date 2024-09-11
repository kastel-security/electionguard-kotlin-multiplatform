package electionguard.encrypt

import electionguard.model.Manifest
import electionguard.model.PlaintextBallot
import electionguard.core.ElGamalPublicKey
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.util.getSystemTimeInMillis
import electionguard.core.hashFunction
import electionguard.core.productionGroup
import electionguard.core.toElementModQ
import electionguard.demonstrate.RandomBallotProvider
import electionguard.demonstrate.buildTestManifest
import electionguard.json.ElementModPJson
import electionguard.json.import
import electionguard.json.publishJson
import electionguard.model.electionBaseHash
import electionguard.model.electionExtendedHash
import electionguard.model.manifestHash
import electionguard.model.parameterBaseHash
import electionguard.util.ErrorMessages
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Verify the embedded nonces in an Encrypted Ballot. */

class EncryptionNonceTest {
    companion object {
        val MANIFEST = buildTestManifest(5, 7)
        val BALLOT_PROVIDER = RandomBallotProvider(MANIFEST, 1)
        const val PUBLIC_KEY = "BDF35018FB4A0A99A268B1940F6815505AA50FB29A49077AB591EF261D03E9035D59316948AE88FF4DCE3" +
                "F2F42252B93B16194D248840BF95FF7F6ADEA3D2AD79949313839AF39A5B909D9803467ECDC68EE1043A03575904085548AA" +
                "BE34F8BB678DE4FCCF9EC3D7DF06C2CF46D40885DC5F8ABEF75B331610BBC0E278EE71681F0A812DCCC70224CC4DB86A6B0E" +
                "B24397C399F88BCD3D03A200FF7A26CEF9374A137586D37C2ECF257D675A9898E572F796B4EE6543E6A48956AD4ADE6BA5B9" +
                "35DAAEF253E215A84DCCC99AD1FC992A5516457553C16D44581485C725F68AD23B000EF37170A18AA2BE23CC58547D20A26B" +
                "2A410A73633EF0754CCF8CF8D0F04CCFB0FD4F3F3F0B5374EE6338B54C57CF0761BF3E26B780F989D6FBFFE42E938AE7F0A1" +
                "BA7F2AA055975B810A029AC4BF9F67EF2590C1890944DE5BAF6C326AE8E634E8048432EE62D6FDA7D1A646D114BF912AD090" +
                "559536B07F9A1A8CFCF5BE5D5F811126308C2F5678EAEB32716954AACA615292A960F099117739C0B1B047C0A8336D314686" +
                "2841356AEC456134DEC6480A4E7748F82C7DC1AE39ED01FBEFBAE95A6839B1AF967164CF744623C9F8C085EA1B551BE3A161" +
                "46570DA88C0ABCBD6907CBEC590A971000A041DC5458D8F89856FFE0F5177D13646B0CF26D9AEA53AA9DB00B8546EB132C71" +
                "80F04D74D6266FD480944833DA0B10B67EAD509"
    }
    val nballots = 11

    fun getExtendedBaseHash(group: GroupContext, publicKey: ElGamalPublicKey): UInt256 {
        val parameterByteHash = parameterBaseHash(group.constants)
        val manifestHash = manifestHash(parameterByteHash,
            Json{prettyPrint=true}.encodeToString(MANIFEST.publishJson()).encodeToByteArray())
        val baseHash = electionBaseHash(parameterByteHash, manifestHash, 3, 2)
        return electionExtendedHash(baseHash, publicKey.key)
    }

    fun getPublicKey(group: GroupContext): ElGamalPublicKey {
        val publicKeyElement: ElementModPJson = Json.decodeFromString("\"$PUBLIC_KEY\"")
        return ElGamalPublicKey(publicKeyElement.import(group)!!)
    }

    @Test
    fun testEncryptionNonces() {
        val group = productionGroup()
        val publicKey = getPublicKey(group)
        val extendedBaseHash = getExtendedBaseHash(group, publicKey)
        // encrypt
        val encryptor = Encryptor(
            group,
            MANIFEST,
            publicKey,
            extendedBaseHash,
            "device"
        )

        val starting = getSystemTimeInMillis()
        RandomBallotProvider(MANIFEST, nballots).ballots().forEach { ballot ->
            val ciphertextBallot = encryptor.encrypt(ballot, ByteArray(0), ErrorMessages("testEncryptionNonces"))!!
            // decrypt with nonces
            val decryptionWithNonce = VerifyEmbeddedNonces(group, MANIFEST, publicKey, extendedBaseHash)
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