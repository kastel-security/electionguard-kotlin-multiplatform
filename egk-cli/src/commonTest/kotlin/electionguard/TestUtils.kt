package electionguard

expect val testResourcesDir: String

expect suspend fun awaitCli(cliBlock: () -> Unit)
