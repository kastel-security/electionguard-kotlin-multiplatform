package electionguard.cli

import electionguard.testResourcesDir
import kotlin.test.Test

/** Test Decryption with in-process DecryptingTrustee's. */
class RunTrustedTallyDecryptionTest {

    @Test
    fun testDecryptionAllJson() {
        RunTrustedTallyDecryption.main(
            arrayOf(
                "-in",
                "$testResourcesDir/workflow/allAvailableJson",
                "-trustees",
                "$testResourcesDir/workflow/allAvailableJson/private_data/trustees",
                "-out",
                "testOut/decrypt/testDecryptionJson",
                "-createdBy",
                "RunTrustedTallyDecryptionTest",
            )
        )
    }

    @Test
    fun testDecryptionSomeJson() {
        RunTrustedTallyDecryption.main(
            arrayOf(
                "-in",
                "$testResourcesDir/workflow/someAvailableJson",
                "-trustees",
                "$testResourcesDir/workflow/someAvailableJson/private_data/trustees",
                "-out",
                "testOut/decrypt/testDecryptionSome",
                "-createdBy",
                "RunTrustedTallyDecryptionTest",
                "-missing",
                "1,4"
            )
        )
    }
}
