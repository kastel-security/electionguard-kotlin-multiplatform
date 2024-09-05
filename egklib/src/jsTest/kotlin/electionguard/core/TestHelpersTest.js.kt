package electionguard.core

import kotlinx.coroutines.async
import node.buffer.BufferEncoding
import node.fs.BigIntStatsFs
import node.fs.StatSyncOptions
import node.fs.readdirSync
import kotlin.test.Test
import kotlin.test.assertTrue

class TestHelpersTest {

    @Test
    fun runTestTest() {
        runTest {
            assertTrue{ true }
        }
        println(readdirSync("../../../../egklib", options = null as BufferEncoding?))
    }
}
