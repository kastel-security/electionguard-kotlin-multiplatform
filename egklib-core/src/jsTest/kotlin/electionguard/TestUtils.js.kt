package electionguard

import electionguard.core.Platform
import electionguard.core.getPlatform

actual val testResourcesDir = "kotlin"

actual fun getTestPlatform() = when(getPlatform()) {
    Platform.BROWSER -> TestPlatform.Browser
    Platform.NODE -> TestPlatform.NodeJs
}
