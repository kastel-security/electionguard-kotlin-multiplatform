package electionguard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

fun main() = runCli {

}

val scope = MainScope()

/**
 * Run a block of code in a blocking manner.
 * For JS, this will launch a coroutine and returns immediately.
 * This is okay since we cannot block in JS and the node process will wait for the promise to resolve.
 */
expect fun runCli(block: suspend CoroutineScope.() -> Unit): Unit
