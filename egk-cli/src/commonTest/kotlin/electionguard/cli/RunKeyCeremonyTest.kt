package electionguard.cli

import electionguard.core.productionGroup
import electionguard.testResourcesDir
import kotlin.test.Test

class RunKeyCeremonyTest {

    @Test
    fun testKeyCeremonyJson() {
        RunTrustedKeyCeremony.main(
            arrayOf(
                "-in", "$testResourcesDir/startConfigJson",
                "-trustees", "testOut/keyceremony/testKeyCeremonyJson/private_data/trustees",
                "-out", "testOut/keyceremony/testKeyCeremonyJson",
            )
        )
        RunVerifier.runVerifier(productionGroup(), "testOut/keyceremony/testKeyCeremonyJson", 1, true)
    }

}

