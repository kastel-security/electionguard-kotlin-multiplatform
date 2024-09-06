package electionguard

import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.constant
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.seconds

/** Generates arbitrary ByteArray of length len. */
fun byteArrays(len: Int): Arb<ByteArray> = Arb.byteArray(Arb.constant(len), Arb.byte())

inline fun <reified T> jsonRoundTrip(value: T): T {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }

    val jsonT: JsonElement = jsonReader.encodeToJsonElement(value)
    val jsonS = jsonT.toString()
    val backToJ: JsonElement = jsonReader.parseToJsonElement(jsonS)
    val backToT: T = jsonReader.decodeFromJsonElement(backToJ)
    return backToT
}

fun runTest(f: suspend TestScope.() -> Unit): TestResult {
    // another benefit of having this wrapper code: we don't have to have the OptIn thing
    // at the top of every unit test file

    //Edit: in order to use runTest in commonTest, we need to return the result - see documentation
    return kotlinx.coroutines.test.runTest(EmptyCoroutineContext, timeout = 101.seconds, f)
}
