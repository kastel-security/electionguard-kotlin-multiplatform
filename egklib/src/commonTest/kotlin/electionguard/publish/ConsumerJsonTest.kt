package electionguard.publish

import electionguard.model.protocolVersion
import electionguard.core.productionGroup
import electionguard.testResourcesDir
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import kotlin.test.Test

class ConsumerJsonTest {

    companion object {
        const val electionScopeId = "TestManifest"
        val topdir = "$testResourcesDir/testElectionRecord/remoteWorkflow/electionRecord"
    }

    //@ParameterizedTest
    @Test
    fun readElectionRecord() {
        val group = productionGroup()
        val electionRecord = readElectionRecord(group, topdir)
        val electionInit = electionRecord.electionInit()

        if (electionInit == null) {
            println("readElectionRecord error $topdir")
        }

        val manifest = electionRecord.manifest()
        println("electionRecord.manifest.specVersion = ${manifest.specVersion}")
        assertEquals(electionScopeId, manifest.electionScopeId)
        assertEquals(protocolVersion, manifest.specVersion)
    }

    //@ParameterizedTest
    @Test
    fun readSpoiledBallotTallys() {
        val group = productionGroup()
        val consumerIn = makeConsumer(group, topdir)
        var count = 0
        for (tally in consumerIn.iterateDecryptedBallots()) {
            println("$count tally = ${tally.id}")
            assertTrue(tally.id.startsWith("ballot-id"))
            count++
        }
    }

    //@ParameterizedTest
    @Test
    fun readEncryptedBallots() {
        val group = productionGroup()
        val consumerIn = makeConsumer(group, topdir)
        var count = 0
        for (ballot in consumerIn.iterateAllEncryptedBallots { true }) {
            println("$count ballot = ${ballot.ballotId}")
            assertTrue(ballot.ballotId.startsWith("id"))
            count++
        }
    }

    //@ParameterizedTest
    @Test
    fun readEncryptedBallotsCast() {
        val group = productionGroup()
        val consumerIn = makeConsumer(group, topdir)
        var count = 0
        for (ballot in consumerIn.iterateAllCastBallots()) {
            println("$count ballot = ${ballot.ballotId}")
            assertTrue(ballot.ballotId.startsWith("id"))
            count++
        }
    }

    //@ParameterizedTest
    @Test
    fun readSubmittedBallotsSpoiled() {
        val group = productionGroup()
        val consumerIn = makeConsumer(group, topdir)
        var count = 0
        for (ballot in consumerIn.iterateAllSpoiledBallots()) {
            println("$count ballot = ${ballot.ballotId}")
            assertTrue(ballot.ballotId.startsWith("ballot-id"))
            count++
        }
    }

}