package electionguard.benchmark

import com.github.michaelbull.result.unwrap
import electionguard.model.ContestData
import electionguard.model.decodeToContestData
import electionguard.model.encodeToByteArray
import electionguard.runTest
import io.kotest.common.ExperimentalKotest
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTime

class ByteArraySerializeBenchmark {

    @OptIn(ExperimentalKotest::class)
    @Test
    // measure performance of platform implementation of serialize and deserialize ContestData
    // currently the js implementation performs quite bad compared to the jvm implementation
    fun serializeAndDeserializeContestData() = runTest {
        val writeIns = Arb.list(Arb.string(56..512, Codepoint.printableAscii()), 0..500)
        val overVotes = Arb.list(Arb.int(0..Int.MAX_VALUE), 0..100)
        val contestData = Arb.bind(overVotes, writeIns) { v, w -> ContestData(v, w) }

        var count = 0
        val time = measureTime {
            checkAll(PropTestConfig(iterations = 100), contestData) { data ->
                val serialized = data.encodeToByteArray()
                val deserialized = serialized.decodeToContestData().unwrap()
                assertEquals(data, deserialized)
                count++
            }
        }
        val perTrip = if (count == 0) 0 else (time / count)
        println(" that took $time for $count round trips - $perTrip per trip")
    }
}
