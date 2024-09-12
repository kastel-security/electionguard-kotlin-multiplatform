package electionguard

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.promise
import kotlin.js.Promise

//To ensure that we can wait for the coroutine in tests, we store all activte Promises from cli runs
var activePromises: MutableList<Promise<Unit>> = emptyList<Promise<Unit>>().toMutableList()

/**
 * Run a block of code in a blocking manner.
 * For JS, this will launch a coroutine and returns immediately.
 * This is okay since we cannot block in JS and the node process will wait for the promise to resolve.
 */
actual fun runCli(block: suspend CoroutineScope.() -> Unit) {
    activePromises.add(scope.promise(start = CoroutineStart.DEFAULT) { block() })
}
