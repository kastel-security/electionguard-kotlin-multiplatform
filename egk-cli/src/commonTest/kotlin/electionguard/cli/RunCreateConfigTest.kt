package electionguard.cli

import electionguard.testResourcesDir
import kotlin.test.Test

class RunCreateConfigTest {

    @Test
    fun testCreateConfigJson() {
        RunCreateElectionConfig.main(
            arrayOf(
                "-manifest",
                "$testResourcesDir/startManifestJson/manifest.json",
                "-nguardians", "3",
                "-quorum", "3",
                "-out",
                "testOut/config/startConfigJson",
                "-device",
                "device information",
            )
        )
    }

    @Test
    fun testCreateConfigDirectoryJson() {
        RunCreateElectionConfig.main(
            arrayOf(
                "-manifest",
                "$testResourcesDir/startManifestJson",
                "-nguardians", "3",
                "-quorum", "3",
                "-createdBy", "testCreateConfigDirectoryJson",
                "-out",
                "testOut/config/testCreateConfigDirectoryJson",
                "-device",
                "device information",
            )
        )
    }

}

