package electionguard

import electionguard.core.*
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.minutes


@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> jsonRoundTrip(value: T): T {
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }

    val jsonT: JsonElement = jsonReader.encodeToJsonElement(value)
    val jsonS = jsonT.toString()
    val backToJ: JsonElement = jsonReader.parseToJsonElement(jsonS)
    val backToT: T = jsonReader.decodeFromJsonElement(backToJ)
    return backToT
}

expect val testResourcesDir: String


/**
 * Kotest requires its properties to be executed as a suspending function. To make this all work,
 * we're using [kotlinx.coroutines.test.runTest] to do it. Note that this internal `runTest`
 * function requires that it be called *at most once per test method*. It's fine to put multiple
 * asserts or `forAll` calls or whatever else inside the `runTest` lambda body.
 */

fun runTest(f: suspend TestScope.() -> Unit): TestResult {
    // another benefit of having this wrapper code: we don't have to have the OptIn thing
    // at the top of every unit test file

    //Edit: in order to use runTest in commonTest, we need to return the result - see documentation
    return kotlinx.coroutines.test.runTest(EmptyCoroutineContext, timeout = 10.minutes, f)
}

/*
fun runTest(f: suspend TestScope.() -> Unit) {
    // another benefit of having this wrapper code: we don't have to have the OptIn thing
    // at the top of every unit test file
    kotlinx.coroutines.test.runTest { f() }
}

 */

/** Verifies that two byte arrays are different. */
fun assertContentNotEquals(a: ByteArray, b: ByteArray, message: String? = null) {
    assertFalse(a.contentEquals(b), message)
}

fun generateRangeChaumPedersenProofKnownNonce(
    context: GroupContext
): ChaumPedersenRangeProofKnownNonce {
    return ChaumPedersenRangeProofKnownNonce(
        listOf(generateGenericChaumPedersenProof(context)),
    )
}

fun generateGenericChaumPedersenProof(context: GroupContext): ChaumPedersenProof {
    return ChaumPedersenProof(generateElementModQ(context), generateElementModQ(context),)
}

fun generateSchnorrProof(context: GroupContext): SchnorrProof {
    return SchnorrProof(
        generatePublicKey(context),
        generateElementModQ(context),
        generateElementModQ(context),
    )
}

fun generateCiphertext(context: GroupContext): ElGamalCiphertext {
    return ElGamalCiphertext(generateElementModP(context), generateElementModP(context))
}

fun generateHashedCiphertext(context: GroupContext): HashedElGamalCiphertext {
    return HashedElGamalCiphertext(generateElementModP(context), "what".encodeToByteArray(), generateUInt256(context), 42)
}

fun generateElementModQ(context: GroupContext): ElementModQ {
    return context.uIntToElementModQ(Random.nextUInt(134217689.toUInt()))
}

fun generateUInt256(context: GroupContext): UInt256 {
    return generateElementModQ(context).toUInt256safe()
}

fun generateElementModP(context: GroupContext) = context.randomElementModP()

fun generatePublicKey(group: GroupContext): ElementModP =
    group.gPowP(group.randomElementModQ())
