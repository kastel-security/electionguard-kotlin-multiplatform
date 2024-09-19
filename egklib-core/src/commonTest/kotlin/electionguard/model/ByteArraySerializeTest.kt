package electionguard.model

import com.github.michaelbull.result.unwrap
import kotlin.test.Test
import kotlin.test.assertEquals

class ByteArraySerializeTest {

    @Test
    fun simpleSerialization() {
        val data = ContestData(listOf(1, 2, 3, 4), listOf("a string"))
        val serialized = data.encodeToByteArray()
        val deserialized = serialized.decodeToContestData().unwrap()
        assertEquals(data, deserialized)
    }
}
