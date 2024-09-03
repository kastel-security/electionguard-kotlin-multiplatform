package electionguard.decrypt

import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.core.*
import electionguard.encrypt.Encryptor
import electionguard.encrypt.submit
import electionguard.input.RandomBallotProvider
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import electionguard.util.ErrorMessages
import electionguard.util.Stats
import electionguard.verifier.VerifyDecryption
import kotlin.math.roundToInt
import kotlin.test.*

/** Test KeyCeremony Trustee generation and recovered decryption. */
@Ignore // I/O is not supported in browser tests
class EncryptDecryptBallotTest {
    val group = productionGroup()
    val configDir = "src/commonTest/data/startConfigJson"
    val outputDir = "testOut/RecoveredDecryptionTest"
    val trusteeDir = "$outputDir/private_data"

    @Test
    fun testAllPresent() {
        runEncryptDecryptBallot(group, configDir, outputDir, trusteeDir, listOf(1, 2, 3, 4, 5)) // all
    }

    @Test
    fun testQuotaPresent() {
        runEncryptDecryptBallot(group, configDir, outputDir, trusteeDir, listOf(2, 3, 4)) // quota
    }

    @Test
    fun testSomePresent() {
        runEncryptDecryptBallot(group, configDir, outputDir, trusteeDir, listOf(1, 2, 3, 4)) // between
    }
}

private val writeout = false
private val nguardians = 4
private val quorum = 3
private val nballots = 3
private val debug = false

fun runEncryptDecryptBallot(
    group: GroupContext,
    configDir: String,
    outputDir: String,
    trusteeDir: String,
    present: List<Int>,
) {
    val electionRecord = readElectionRecord(group, configDir)
    val config = electionRecord.config()

    //// simulate key ceremony
    val trustees: List<KeyCeremonyTrustee> = List(nguardians) {
        val seq = it + 1
        KeyCeremonyTrustee(group, "guardian$seq", seq, nguardians, quorum)
    }.sortedBy { it.xCoordinateAttribute }
    trustees.forEach { t1 ->
        trustees.forEach { t2 ->
            t1.receivePublicKeys(t2.publicKeys().unwrap())
        }
    }
    trustees.forEach { t1 ->
        trustees.filter { it.idAttribute != t1.idAttribute }.forEach { t2 ->
            t2.receiveEncryptedKeyShare(t1.encryptedKeyShareFor(t2.idAttribute).unwrap())
        }
    }
    val guardianList: List<Guardian> = trustees.map { makeGuardian(it) }
    val guardians = Guardians(group, guardianList)
    val jointPublicKey: ElementModP =
        trustees.map { it.guardianPublicKey() }.reduce { a, b -> a * b }

    val extendedBaseHash = electionExtendedHash(config.electionBaseHash, jointPublicKey)
    val dTrustees: List<DecryptingTrusteeDoerre> = trustees.map { makeDoerreTrustee(it, extendedBaseHash) }

    testEncryptDecryptVerify(
        group,
        electionRecord.manifest(),
        UInt256.TWO,
        ElGamalPublicKey(jointPublicKey),
        guardians,
        dTrustees,
        present
    )

    //////////////////////////////////////////////////////////
    if (writeout) {
        val init = ElectionInitialized(
            config,
            jointPublicKey,
            extendedBaseHash,
            guardianList,
        )

        val publisher = makePublisher(outputDir)
        publisher.writeElectionInitialized(init)

        val trusteePublisher = makePublisher(trusteeDir)
        trustees.forEach { trusteePublisher.writeTrustee(trusteeDir, it) }
    }
}

fun testEncryptDecryptVerify(
    group: GroupContext,
    manifest: Manifest,
    extendedBaseHash: UInt256,
    publicKey: ElGamalPublicKey,
    guardians: Guardians,
    trustees: List<DecryptingTrusteeDoerre>,
    present: List<Int>
) {
    println("present $present")

    val available = trustees.filter { present.contains(it.xCoordinate()) }
    val encryptor = Encryptor(group, manifest, publicKey, extendedBaseHash, "device")
    val decryptor = DecryptorDoerre(group, extendedBaseHash, publicKey, guardians, available)
    val verifier = VerifyDecryption(group, manifest, publicKey, extendedBaseHash)

    var encryptTime = 0L
    var decryptTime = 0L
    RandomBallotProvider(manifest, nballots).withWriteIns().ballots().forEach { ballot ->
        val startEncrypt = getSystemTimeInMillis()
        val ciphertextBallot = encryptor.encrypt(ballot, ByteArray(0), ErrorMessages("testEncryptDecryptVerify"))
        val encryptedBallot = ciphertextBallot!!.submit(EncryptedBallot.BallotState.CAST)
        encryptTime += getSystemTimeInMillis() - startEncrypt

        val startDecrypt = getSystemTimeInMillis()
        val errs = ErrorMessages("testEncryptDecryptVerify")
        val decryptedBallot = decryptor.decryptBallot(encryptedBallot, errs)
        if (decryptedBallot == null) {
            println("testEncryptDecryptVerify failedf errors = $errs")
            return
        }
        decryptTime += getSystemTimeInMillis() - startDecrypt

        // contestData matches
        ballot.contests.forEach { orgContest ->
            val mcontest = manifest.contests.find { it.contestId == orgContest.contestId }!!
            val orgContestData = makeContestData(mcontest.votesAllowed, orgContest.selections, orgContest.writeIns)

            val dcontest = decryptedBallot.contests.find { it.contestId == orgContest.contestId }
            assertNotNull(dcontest)
            assertNotNull(dcontest.decryptedContestData)
            assertEquals(dcontest.decryptedContestData!!.contestData.writeIns, orgContestData.writeIns)
            println("   ${orgContest.contestId} writeins = ${orgContestData.writeIns}")

            val status = dcontest.decryptedContestData!!.contestData.status
            val overvotes = dcontest.decryptedContestData!!.contestData.overvotes
            if (debug) println(" status = $status overvotes = $overvotes")

            // check if selection votes match
            orgContest.selections.forEach { selection ->
                val dselection = dcontest.selections.find { it.selectionId == selection.selectionId }

                if (status == ContestDataStatus.over_vote) {
                    // check if overvote was correctly recorded
                    val hasWriteIn = overvotes.find { it == selection.sequenceOrder } != null
                    assertEquals(selection.vote == 1, hasWriteIn)

                } else {
                    // check if selection votes match
                    assertNotNull(dselection)
                    assertEquals(selection.vote, dselection.tally)
                }
            }

            verifier.verify(decryptedBallot,  true, errs.nested("verify"), Stats())
            println(errs)
            assertFalse(errs.hasErrors())
        }
    }

    val encryptPerBallot = (encryptTime.toDouble() / nballots).roundToInt()
    val decryptPerBallot = (decryptTime.toDouble() / nballots).roundToInt()
    println("testDecryptor for $nballots ballots took $encryptPerBallot encrypt, $decryptPerBallot decrypt msecs/ballot")
}