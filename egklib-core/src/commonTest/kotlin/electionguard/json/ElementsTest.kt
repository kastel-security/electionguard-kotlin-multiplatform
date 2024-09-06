package electionguard.json

import electionguard.core.*
import electionguard.core.Base16.fromHex
import io.kotest.property.checkAll
import kotlinx.coroutines.test.TestResult
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import kotlin.test.*


@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> jsonRoundTripWithStringPrimitive(value: T): T {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }

    val jsonT: JsonElement = jsonReader.encodeToJsonElement(value)

    if (jsonT is JsonPrimitive) {
        assertTrue(jsonT.isString)
        assertNotNull(jsonT.content.fromHex()) // validates that we have a base16 string
    } else {
        fail("expected jsonT to be JsonPrimitive")
    }

    val jsonS = jsonT.toString()
    val backToJ: JsonElement = jsonReader.parseToJsonElement(jsonS)
    val backToT: T = jsonReader.decodeFromJsonElement(backToJ)
    return backToT
}

class ElementsTest {
    @Test
    fun testElementRoundtrip(): TestResult {
        return runTest {
            val group = productionGroup()
            checkAll(elementsModP(group), elementsModQ(group)) { p, q ->
                assertEquals(p, p.publishJson().import(group))
                assertEquals(q, q.publishJson().import(group))

                // longer round-trip through serialized JSON strings and back
                assertEquals(p, jsonRoundTripWithStringPrimitive(p.publishJson()).import(group))
                assertEquals(q, jsonRoundTripWithStringPrimitive(q.publishJson()).import(group))
            }
        }
    }

    @Test
    fun importTinyElements(): TestResult {
        return runTest {
            val group = tinyGroup()
            checkAll(elementsModP(group), elementsModQ(group)) { p, q ->
                // shorter round-trip from the core classes to JsonElement and back
                assertEquals(p, p.publishJson().import(group))
                assertEquals(q, q.publishJson().import(group))

                // longer round-trip through serialized JSON strings and back
                assertEquals(p, jsonRoundTripWithStringPrimitive(p.publishJson()).import(group))
                assertEquals(q, jsonRoundTripWithStringPrimitive(q.publishJson()).import(group))
            }
        }
    }

    @Test
    fun testUInt256Roundtrip(): TestResult {
        return runTest {
            val context = productionGroup()
            checkAll(elementsModQ(context)) { q ->
                val u : UInt256 = q.toUInt256safe()
                assertEquals(u, u.publishJson().import())
                assertEquals(u, jsonRoundTripWithStringPrimitive(u.publishJson()).import())
            }
        }
    }
}