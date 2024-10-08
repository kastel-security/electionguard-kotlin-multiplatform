package electionguard.encrypt

import electionguard.core.productionGroup
import electionguard.testResourcesDir
import electionguard.demonstrate.RandomBallotProvider
import electionguard.encrypt.AddEncryptedBallot
import electionguard.model.EncryptedBallot
import electionguard.publish.makePublisher
import electionguard.publish.readElectionRecord
import electionguard.util.ErrorMessages
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class AddBallotSyncTest {
    val group = productionGroup()
    val inputProto = "$testResourcesDir/workflow/allAvailableProto"
    val inputJson = "$testResourcesDir/workflow/allAvailableJson"
    val outputDirTop = "testOut/encrypt/AddBallotSyncTest"

    val nballots = 4

    @Test
    fun testJsonSyncNoChain() {
        val outputDir = "$outputDirTop/testJsonSyncNoChain"
        val device = "device1"

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, inputJson)
        val electionInit = electionRecord.electionInit()!!
        val publisher = makePublisher(outputDir, true, true)
        publisher.writeElectionInitialized(electionInit)

        val encryptor = AddEncryptedBallot(
            group,
            electionRecord.manifest(),
            electionInit.config.chainConfirmationCodes,
            electionInit.config.configBaux0,
            electionInit.jointElGamalPublicKey(),
            electionInit.extendedBaseHash,
            device,
            outputDir,
            "outputDir/invalidDir",
            isJson = publisher.isJson(),
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        repeat(3) {
            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val result = encryptor.encrypt(ballot, ErrorMessages("testJsonSyncNoChain"))
                assertNotNull(result)
                encryptor.submit(result.confirmationCode, EncryptedBallot.BallotState.CAST)
            }
            encryptor.sync()
        }
        encryptor.close()

        checkOutput(group, outputDir, 3 * nballots, false)

        // should fail once closed
        val ballot = ballotProvider.makeBallot()
        val errs = ErrorMessages("")
        encryptor.encrypt(ballot, errs)
        assertTrue(errs.hasErrors())
        assertContains(errs.toString(), "Trying to add ballot after chain has been closed")
    }

    @Test
    fun testJsonSyncChain() {
        val outputDir = "$outputDirTop/testJsonSyncChain"
        val device = "device1"

        val group = productionGroup()
        val electionRecord = readElectionRecord(group, inputJson)
        val configWithChaining = electionRecord.config().copy(chainConfirmationCodes = true)
        val electionInit = electionRecord.electionInit()!!.copy(config = configWithChaining)

        val publisher = makePublisher(outputDir, true, true)
        publisher.writeElectionInitialized(electionInit)

        val encryptor = AddEncryptedBallot(
            group,
            electionRecord.manifest(),
            electionInit.config.chainConfirmationCodes,
            electionInit.config.configBaux0,
            electionInit.jointElGamalPublicKey(),
            electionInit.extendedBaseHash,
            device,
            outputDir,
            "outputDir/invalidDir",
            isJson = publisher.isJson(),
        )
        val ballotProvider = RandomBallotProvider(electionRecord.manifest())

        repeat(3) {
            repeat(nballots) {
                val ballot = ballotProvider.makeBallot()
                val result = encryptor.encrypt(ballot, ErrorMessages("testJsonSyncChain"))
                assertNotNull(result)
                encryptor.submit(result.confirmationCode, EncryptedBallot.BallotState.CAST)
            }
            encryptor.sync()
        }
        encryptor.close()

        checkOutput(group, outputDir, 3 * nballots, true)

        // should fail once closed
        val ballot = ballotProvider.makeBallot()
        val errs = ErrorMessages("")
        encryptor.encrypt(ballot, errs)
        assertTrue(errs.hasErrors())
        assertContains(errs.toString(), "Trying to add ballot after chain has been closed")
    }

}