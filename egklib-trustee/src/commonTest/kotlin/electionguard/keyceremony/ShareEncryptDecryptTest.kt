package electionguard.keyceremony

import com.github.michaelbull.result.unwrap
import electionguard.core.*
import electionguard.elementsModQ
import electionguard.model.EncryptedKeyShare
import electionguard.model.PublicKeys
import electionguard.propTestFastConfig
import electionguard.runTest
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ShareEncryptDecryptTest {
    @Test
    fun ShareEncryptDecryptFuzzTest(): TestResult {
        return runTest {
            val group = productionGroup()
            checkAll(
                propTestFastConfig,
                Arb.int(min=1, max=100),
                elementsModQ(group, minimum = 2)
            ) { xcoord, pil ->
                val trustee1 = KeyCeremonyTrustee(group, "id1", xcoord, 4, 4)
                val trustee2 = KeyCeremonyTrustee(group, "id2", xcoord+1, 4, 4)

                val publicKeys2 : PublicKeys = trustee2.publicKeys().unwrap()
                val share : HashedElGamalCiphertext = trustee1.shareEncryption(pil, publicKeys2)
                val encryptedShare = EncryptedKeyShare(trustee1.xCoordinate(), trustee1.id(), trustee2.id(), share)

                val pilbytes : ByteArray? = trustee2.shareDecryption(encryptedShare)
                assertNotNull(pilbytes)
                val decodedPil: ElementModQ = pilbytes.toUInt256safe().toElementModQ(group) // Pi(ℓ)
                assertEquals(pil, decodedPil)
            }
        }
    }
}
