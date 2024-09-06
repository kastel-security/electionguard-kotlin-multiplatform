package electionguard.model

import com.github.michaelbull.result.unwrap
import electionguard.core.runTest
import io.kotest.common.ExperimentalKotest
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.DurationUnit
import kotlin.time.measureTime

class ByteArraySerializeTest {

    @OptIn(ExperimentalKotest::class)
    @Test
    // measure performance of platform implementation of serialize and deserialize ContestData
    // currently the js implementation performs quite bad compared to the jvm implementation
    fun serializeAndDeserializeContestData() = runTest {
        val writeIns = Arb.list(Arb.string(56..512, Codepoint.printableAscii()), 0..500)
        val overVotes = Arb.list(Arb.int(0..Int.MAX_VALUE), 0..100)
        val contestData = Arb.bind(overVotes, writeIns) { v, w -> ContestData(v, w) }

        var count = 0
        val took = measureTime {
            checkAll(PropTestConfig(iterations = 100), contestData) { data ->
                testSerialization(data).also { count++ }
            }
        }.toDouble(DurationUnit.MILLISECONDS)
        val perTrip = if (count == 0) 0 else (took / count).roundToInt()
        println(" that took $took millisecs for $count roundtrips = $perTrip msecs/trip wallclock")
    }

    private fun testSerialization(data: ContestData, fill: String? = null) {
        val serialized = data.encodeToByteArray(fill)
        val deserialized = serialized.decodeToContestData().unwrap()
        assertEquals(data, deserialized)
    }

    @Test
    fun simpleSerialization() {
        val data = ContestData(listOf(1, 2, 3, 4), listOf("a string"))
        testSerialization(data)
    }
}
