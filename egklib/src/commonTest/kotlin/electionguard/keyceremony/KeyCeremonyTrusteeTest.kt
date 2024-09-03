package electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.core.*
import kotlin.test.*
import kotlin.test.assertEquals

@Ignore // causes the browser to crash when executing jsTest - may when calling group.generatePolynomial()
class KeyCeremonyTrusteeTest {

    @Test
    @Ignore
    fun testPublicKeys() {
        val group = productionGroup()
        val trustee = KeyCeremonyTrustee(group, "id", 42, 4, 4)
        assertEquals("id", trustee.id())
        assertEquals(42, trustee.xCoordinate())
        assertNotNull(trustee.guardianPublicKey())
        assertNotNull(trustee.electionPrivateKey())
        assertEquals(4, trustee.coefficientCommitments().size)
        assertEquals(trustee.guardianPublicKey(), trustee.coefficientCommitments()[0])

        val result = trustee.publicKeys()
        assertTrue(result is Ok)
        val keys = result.unwrap()
        assertEquals(trustee.id(), keys.guardianId)
        assertEquals(trustee.xCoordinate(), keys.guardianXCoordinate)
        assertEquals(trustee.coefficientCommitments(), keys.coefficientCommitments())
        assertEquals(4, keys.coefficientProofs.size)
    }

    @Test
    @Ignore
    fun testReceivePublicKeysBadQuorum() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 41, 5, 4)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 42, 5, 5)

        val result1 = trustee1.receivePublicKeys(trustee2.publicKeys().unwrap())
        assertTrue(result1 is Err)
        assertEquals("id1 receivePublicKeys from 'id2': needs (4) coefficientProofs", result1.error)
    }

    @Test
    @Ignore
    fun testReceivePublicKeysBadReceiver() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 41, 5, 4)
        val result1 = trustee1.receivePublicKeys(trustee1.publicKeys().unwrap())
        assertTrue(result1 is Err)
        assertEquals("Cant send 'id1' public keys to itself", result1.error)
    }

    @Test
    @Ignore
    fun testReceivePublicKeysBadProofs() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 41, 5, 4)
        val goodProof = trustee1.coefficientProofs()[0]
        val badProof = goodProof.copy(challenge = group.TWO_MOD_Q)
        val badProofs = trustee1.coefficientProofs().toMutableList()
        badProofs[0] = badProof

        val badKeys = PublicKeys("bad", 43, badProofs)
        val result1 = trustee1.receivePublicKeys(badKeys)
        assertTrue(result1 is Err)
        assertContains(result1.error,"  Guardian bad has invalid proof for coefficient 0 inBoundsU=true validChallenge=false")
    }

    @Test
    @Ignore
    fun testShareEncryptDecrypt() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 41, 5, 4)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 42, 5, 4)

        val publicKeys2 : PublicKeys = trustee2.publicKeys().unwrap()
        val pil = group.randomElementModQ()
        val share : HashedElGamalCiphertext = trustee1.shareEncryption(pil, publicKeys2)
        val encryptedShare = EncryptedKeyShare(trustee1.xCoordinate(), trustee1.id(), trustee2.id(), share)

        val pilbytes : ByteArray? = trustee2.shareDecryption(encryptedShare)
        assertNotNull(pilbytes)
        val decodedPil: ElementModQ = pilbytes.toUInt256safe().toElementModQ(group) // Pi(ℓ)
        assertEquals(pil, decodedPil)
    }

    @Test
    @Ignore
    fun testReceiveSecretKeyShare() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 41, 5, 4)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 42, 5, 4)

        val result1 = trustee1.receivePublicKeys(trustee2.publicKeys().unwrap())
        assertTrue(result1 is Ok)

        val result1b = trustee1.encryptedKeyShareFor("bad")
        assertTrue(result1b is Err)
        assertEquals("Trustee 'id1', does not have public key for 'bad'", result1b.error)

        /** Create trustee1 SecretKeyShare for trustee2. */
        val result2 = trustee1.encryptedKeyShareFor(trustee2.id())
        assertTrue(result2 is Ok, result2.toString())
        val ss2 = result2.unwrap()
        assertEquals(trustee1.id(), ss2.polynomialOwner)
        assertEquals(trustee2.id(), ss2.secretShareFor)

        val result3 = trustee2.receiveEncryptedKeyShare(ss2)
        assertTrue(result3 is Err)
        assertEquals("Trustee 'id2' does not have public keys for missingGuardianId 'id1'", result3.error)

        trustee2.receivePublicKeys(trustee1.publicKeys().unwrap())
        val result4 = trustee2.receiveEncryptedKeyShare(ss2)
        assertTrue(result4 is Ok)

        val result5 = trustee1.receiveEncryptedKeyShare(ss2)
        assertTrue(result5 is Err)
        assertEquals("ReceiveEncryptedKeyShare 'id1' sent share to wrong trustee 'id1', should be availableGuardianId 'id2'", result5.error)
    }

    @Test
    @Ignore
    fun testValidateSecretKeyShare() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 41, 5, 4)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 42, 5, 4)

        assertTrue(trustee1.receivePublicKeys(trustee2.publicKeys().unwrap()) is Ok)
        assertTrue(trustee2.receivePublicKeys(trustee1.publicKeys().unwrap()) is Ok)

        /** Create trustee1 SecretKeyShare for trustee2, send to trustee2. */
        val result2 = trustee1.encryptedKeyShareFor(trustee2.id())
        assertTrue(result2 is Ok, result2.toString())
        val ss12 = result2.unwrap()
        val result3 = trustee2.receiveEncryptedKeyShare(ss12)
        assertTrue(result3 is Ok)

        /** Create trustee2 SecretKeyShare for trustee1, send to trustee1. */
        val result1 = trustee2.encryptedKeyShareFor(trustee1.id())
        assertTrue(result1 is Ok)
        val ss21 = result1.unwrap()
        val result4 = trustee1.receiveEncryptedKeyShare(ss21)
        assertTrue(result4 is Ok)

        // mess this one up so it wont decrypt
        val ss2b1 = ss21.copy(encryptedCoordinate = ss12.encryptedCoordinate)
        val result5 = trustee1.receiveEncryptedKeyShare(ss2b1)
        assertTrue(result5 is Err)
        assertEquals("Trustee 'id1' couldnt decrypt EncryptedKeyShare for missingGuardianId 'id2'", result5.error)

        // mess this one up so it wont validate
        val trustee3 = KeyCeremonyTrustee(group, "id3", 43, 5, 4)
        assertTrue(trustee1.receivePublicKeys(trustee3.publicKeys().unwrap()) is Ok)
        // give it the wrong generatingGuardianId
        val ss2v1 = ss21.copy(polynomialOwner = "id3")
        val result7 = trustee1.receiveEncryptedKeyShare(ss2v1)
        assertTrue(result7 is Err)
        assertEquals("Trustee 'id1' error validating EncryptedKeyShare for missingGuardianId 'id3'", result7.error)
    }

    @Test
    @Ignore
    fun testKeyShareFor() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 41, 5, 4)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 42, 5, 4)

        assertTrue(trustee1.receivePublicKeys(trustee2.publicKeys().unwrap()) is Ok)
        assertTrue(trustee2.receivePublicKeys(trustee1.publicKeys().unwrap()) is Ok)

        /** Create trustee1 SecretKeyShare for trustee2. */
        val result2 = trustee1.encryptedKeyShareFor(trustee2.id())
        assertTrue(result2 is Ok, result2.toString())
        val result3 = trustee1.keyShareFor(trustee2.id())
        assertTrue(result3 is Ok)

        /** Create trustee2 SecretKeyShare for trustee1. */
        val result4 = trustee2.encryptedKeyShareFor(trustee1.id())
        assertTrue(result4 is Ok)
        val result5 = trustee2.keyShareFor(trustee1.id())
        assertTrue(result5 is Ok)

        val result6 = trustee1.keyShareFor("bad")
        assertTrue(result6 is Err)
        assertEquals("Trustee 'id1', does not have KeyShare for 'bad'; must call encryptedKeyShareFor() first", result6.error)
    }

    @Test
    @Ignore
    fun testReceiveKeyShare() {
        val group = productionGroup()
        val trustee1 = KeyCeremonyTrustee(group, "id1", 41, 5, 4)
        val trustee2 = KeyCeremonyTrustee(group, "id2", 42, 5, 4)

        assertTrue(trustee1.receivePublicKeys(trustee2.publicKeys().unwrap()) is Ok)
        assertTrue(trustee2.receivePublicKeys(trustee1.publicKeys().unwrap()) is Ok)

        /** Create trustee1 SecretKeyShare for trustee2. */
        val result1 = trustee1.encryptedKeyShareFor(trustee2.id())
        assertTrue(result1 is Ok, result1.toString())
        assertTrue(trustee2.receiveEncryptedKeyShare(result1.unwrap()) is Ok)

        /** Create trustee2 SecretKeyShare for trustee1. */
        val result2 = trustee2.encryptedKeyShareFor(trustee1.id())
        assertTrue(result2 is Ok)
        assertTrue(trustee1.receiveEncryptedKeyShare(result2.unwrap()) is Ok)

        // challenge those by asking for the unencrypted version
        val result3 = trustee1.keyShareFor(trustee2.id())
        assertTrue(result3 is Ok)
        val keyShare12 = result3.unwrap()

        val result4 = trustee2.keyShareFor(trustee1.id())
        assertTrue(result4 is Ok)
        val keyShare21 = result4.unwrap()

        // The normal success cases
        val t1 = trustee2.receiveKeyShare(keyShare12)
        assertTrue(t1 is Ok, "$t1 $keyShare12")
        assertTrue(trustee1.receiveKeyShare(keyShare21) is Ok)

        // give it to the wrong trustee
        val resultWrongTrustee = trustee1.receiveKeyShare(keyShare12)
        assertTrue(resultWrongTrustee is Err)
        assertEquals("Sent KeyShare to wrong trustee 'id1', should be availableGuardianId 'id2'", resultWrongTrustee.error)

        // Give it a bad guardian id
        val keyShareBadId = keyShare12.copy(polynomialOwner = "badId")
        val resultBadId = trustee2.receiveKeyShare(keyShareBadId)
        assertTrue(resultBadId is Err)
        assertTrue(resultBadId.error.contains("Trustee 'id2', does not have public key for missingGuardianId 'badId'"))

        // Give it a bad coordinate
        val keyShareBadCoordinate = keyShare12.copy(yCoordinate = group.TWO_MOD_Q)
        val resultBadCoordinate = trustee2.receiveKeyShare(keyShareBadCoordinate)
        assertTrue(resultBadCoordinate is Err)
        println("result = $resultBadCoordinate")
        assertTrue(resultBadCoordinate.error.contains("Trustee 'id2' error validating KeyShare for missingGuardianId 'id1'"))

        /* Give it a bad nonce LOOK this is disabled in KeyCeremonyTrustee.receiveKeyShare(), see notes there
        val keyShareBadNonce = keyShare12.copy(nonce = group.TWO_MOD_Q)
        val resultBadNonce = trustee2.receiveKeyShare(keyShareBadNonce)
        assertTrue(resultBadNonce is Err)
        assertEquals("Trustee 'id2' couldnt decrypt encryptedKeyShare for missingGuardianId 'id1'", resultBadNonce.error)
         */
    }
}