package electionguard

import electionguard.core.Platform
import electionguard.core.getPlatform
import kotlinx.coroutines.await

actual val testResourcesDir = "kotlin"

actual suspend fun awaitCli(cliBlock: () -> Unit) {
    cliBlock()
    activePromises.forEach { it.await() }
    activePromises.clear()
}
