package electionguard.ballot

import com.github.michaelbull.result.unwrap
import kotlin.test.Test
import kotlin.test.assertEquals

class ByteArraySerializeTest {

    @Test
    fun serializeAndDeserializeContestData() {
        testSerialization(ContestData(listOf(), listOf()))
        testSerialization(ContestData(listOf(1, 2, 3), listOf("a string")))
        testSerialization(
            ContestData(
                listOf(1111, 232, 232323, 14),
                listOf("a long string ", "another string", "a longer string".repeat(100))
            )
        )
        testSerialization(
            ContestData(
                listOf(Int.MAX_VALUE, 323, 232323, 0),
                listOf("a long string ", "another string", "a longer string".repeat(100)),
                ContestDataStatus.over_vote
            )
        )
    }

    private fun testSerialization(data: ContestData, fill: String? = null) {
        val serialized = data.encodeToByteArray(fill)
        val deserialized = serialized.decodeToContestData().unwrap()
        assertEquals(data, deserialized)
    }
}
