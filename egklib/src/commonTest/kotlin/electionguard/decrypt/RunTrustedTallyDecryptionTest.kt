package electionguard.decrypt

import electionguard.core.productionGroup

import kotlin.test.Test

/** Test Decryption with in-process DecryptingTrustee's. */
class RunTrustedTallyDecryptionTest {
    @Test
    fun testDecryptionAll() {
        val group = productionGroup()
        val inputDir = "src/commonTest/data/allAvailable"
        val trusteeDir = "src/commonTest/data/allAvailable/private_data/trustees"
        val outputDir = "testOut/decrypt/testDecryptionAll"
        println("testDecryptionAll input= $inputDir\n   trustees= $trusteeDir\n   output = $outputDir")
        runDecryptTally(group, inputDir, outputDir, readDecryptingTrustees(group, inputDir, trusteeDir), "createdBy")
    }

    @Test
    fun testDecryptionJson() {
        main(
            arrayOf(
                "-in",
                "testOut/tally/testRunBatchEncryptionJson",
                "-trustees",
                "testOut/keyceremony/testKeyCeremonyJson/private_data/trustees",
                "-out",
                "testOut/decrypt/testDecryptionJson",
                "-createdBy",
                "RunTrustedTallyDecryptionTest",
                "-missing",
                "1,4"
            )
        )
    }

    @Test
    fun testDecryptionSome() {
        main(
            arrayOf(
                "-in",
                "src/commonTest/data/someAvailable",
                "-trustees",
                "src/commonTest/data/someAvailable/private_data/trustees",
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
