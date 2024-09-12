package electionguard

actual val testResourcesDir = "src/commonTest/resources"

actual suspend fun awaitCli(cliBlock: () -> Unit) {
    cliBlock()
}
