package electionguard.encrypt

import electionguard.core.*
import electionguard.demonstrate.RandomBallotProvider
import electionguard.demonstrate.buildTestManifest
import electionguard.json.ElementModPJson
import electionguard.json.import
import electionguard.json.publishJson
import electionguard.model.electionBaseHash
import electionguard.model.electionExtendedHash
import electionguard.model.manifestHash
import electionguard.model.parameterBaseHash
import kotlin.time.Duration.Companion.seconds
import electionguard.util.ErrorMessages
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class EncryptTest {
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

    fun getEncryptor(group: GroupContext): Encryptor {
        val publicKeyElement: ElementModPJson = Json.decodeFromString("\"$PUBLIC_KEY\"")
        val publicKey = ElGamalPublicKey(publicKeyElement.import(group)!!)
        val parameterByteHash = parameterBaseHash(group.constants)
        val manifestHash = manifestHash(parameterByteHash,
            Json{prettyPrint=true}.encodeToString(MANIFEST.publishJson()).encodeToByteArray())
        val baseHash = electionBaseHash(parameterByteHash, manifestHash, 3, 2)
        val extendedBaseHash = electionExtendedHash(baseHash, publicKey.key)
        return Encryptor(group, MANIFEST, publicKey, extendedBaseHash, "device")
    }

    // sanity check that encryption doesnt barf
    @Test
    fun testEncryption(): TestResult {
        return runTest(timeout = 101.seconds) {
            val group = productionGroup()
            val ballot = BALLOT_PROVIDER.makeBallot()
            val encryptor = getEncryptor(group)

            val result = encryptor.encrypt(ballot, ByteArray(0), ErrorMessages("testEncryption"))!!

            var first = true
            println("result = ${result.confirmationCode} nonce ${result.ballotNonce}")
            for (contest in result.contests) {
                // println(" contest ${contest.contestId} = ${contest.cryptoHash} nonce ${contest.contestNonce}")
                for (selection in contest.selections) {
                    // println("  selection ${selection.selectionId} = ${selection.cryptoHash} nonce ${selection.selectionNonce}")
                    if (first) println("\n*****first ${selection}\n")
                    first = false
                }
            }
        }
    }

    // test that if you pass in the same ballot nonce, you get the same encryption
    @Test
    fun testEncryptionWithBallotNonce(): TestResult {
        return runTest {
            val group = productionGroup()
            val ballot = BALLOT_PROVIDER.makeBallot()
            val encryptor = getEncryptor(group)

            val nonce1 = UInt256.random()
            val result1 = encryptor.encrypt(ballot, ByteArray(0), ErrorMessages("testEncryptionWithBallotNonce1"), nonce1, 0)!!
            val result2 = encryptor.encrypt(ballot, ByteArray(0), ErrorMessages("testEncryptionWithBallotNonce2"), nonce1, 0)!!

            result1.contests.forEachIndexed { index, contest1 ->
                val contest2 = result2.contests[index]
                contest1.selections.forEachIndexed { sindex, selection1 ->
                    val selection2 = contest2.selections[sindex]
                    assertEquals(selection1, selection2)
                }
                assertEquals(contest1, contest2)
            }
            // data class equals doesnt compare bytearray.contentEquals()
            assertEquals(result1.confirmationCode, result2.confirmationCode)
            assertEquals(result1.timestamp, result2.timestamp)
            assertEquals(result1.ballotNonce, result2.ballotNonce)
        }
    }

    // test sn encryption
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testEncryptionWithSN(): TestResult {
        return runTest {
            val group = productionGroup()
            val ballot = BALLOT_PROVIDER.makeBallot()
            val plaintextSn: Int? = ballot.sn?.toInt()

            val publicKeyElement: ElementModPJson = Json.decodeFromString("\"$PUBLIC_KEY\"")
            val publicKey = ElGamalPublicKey(publicKeyElement.import(group)!!)
            val parameterByteHash = parameterBaseHash(group.constants)
            val manifestHash = manifestHash(parameterByteHash,
                Json{prettyPrint=true}.encodeToString(MANIFEST.publishJson()).encodeToByteArray())
            val baseHash = electionBaseHash(parameterByteHash, manifestHash, 3, 2)
            val extendedBaseHash = electionExtendedHash(baseHash, publicKey.key)
            val encryptor = Encryptor(group, MANIFEST, publicKey, extendedBaseHash, "device")

            val nonce1 = UInt256.random()
            val result1 = encryptor.encrypt(
                ballot,
                ByteArray(0),
                ErrorMessages("testEncryptionWithBallotNonce1"),
                nonce1,
                0,
            )!!
            val result2 = encryptor.encrypt(
                ballot,
                ByteArray(0),
                ErrorMessages("testEncryptionWithBallotNonce2"),
                nonce1,
                0,
            )!!

            result1.contests.forEachIndexed { index, contest1 ->
                val contest2 = result2.contests[index]
                contest1.selections.forEachIndexed { sindex, selection1 ->
                    val selection2 = contest2.selections[sindex]
                    assertEquals(selection1, selection2)
                }
                assertEquals(contest1, contest2)
            }
            // data class equals doesnt compare bytearray.contentEquals()
            assertEquals(result1.confirmationCode, result2.confirmationCode)
            assertEquals(result1.timestamp, result2.timestamp)
            assertEquals(result1.ballotNonce, result2.ballotNonce)
            assertEquals(result1.encryptedSN, result2.encryptedSN)

            // test decrypt with nonce

            val snNonce = hashFunction(extendedBaseHash.bytes, 0x110.toByte(), nonce1).toElementModQ(group)
            val dvalue : Int? = result1.encryptedSN!!.decryptWithNonce(publicKey, snNonce)
            assertNotNull(dvalue)
            assertEquals(plaintextSn, dvalue)
        }
    }
}