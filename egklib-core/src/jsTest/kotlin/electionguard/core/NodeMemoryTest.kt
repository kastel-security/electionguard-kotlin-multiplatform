package electionguard.core

import kotlin.test.Test
import kotlin.test.assertTrue

class NodeMemoryTest {

    @Test
    // check if the node options are configured correctly
    fun checkNodeMemory() {
        if (getPlatform() == Platform.NODE) {
            val heapStats = require("v8").getHeapStatistics().unsafeCast<HeapStatistics>()

            println("Heap Size Limit: ${heapStats.heap_size_limit / 1024 / 1024} MB")
            println("Total Heap Size: ${heapStats.total_heap_size / 1024 / 1024} MB")
            println("Used Heap Size: ${heapStats.used_heap_size / 1024 / 1024} MB")
            assertTrue { heapStats.total_available_size >= 4096 }
        }
    }
}

interface HeapStatistics {
    val heap_size_limit: Double
    val total_heap_size: Double
    val total_available_size: Double
    val used_heap_size: Double
}
