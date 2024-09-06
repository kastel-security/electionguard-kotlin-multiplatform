package electionguard

import com.github.michaelbull.result.unwrap
import electionguard.core.*
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.model.Guardian
import io.kotest.common.ExperimentalKotest
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.ShrinkingMode
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.map
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.time.Duration.Companion.seconds

fun makeDoerreTrustee(ktrustee: KeyCeremonyTrustee, electionId : UInt256): DecryptingTrusteeDoerre {
    return DecryptingTrusteeDoerre(
        ktrustee.id(),
        ktrustee.xCoordinate(),
        ktrustee.guardianPublicKey(),
        ktrustee.computeSecretKeyShare(),
    )
}

fun makeGuardian(trustee: KeyCeremonyTrustee): Guardian {
    val publicKeys = trustee.publicKeys().unwrap()
    return Guardian(
        trustee.id(),
        trustee.xCoordinate(),
        publicKeys.coefficientProofs,
    )
}

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
    return runTest(EmptyCoroutineContext, timeout = 101.seconds, f)
}

/** Generate an arbitrary ElementModQ in [minimum, Q) for the given group context. */
fun elementsModQ(ctx: GroupContext, minimum: Int = 0): Arb<ElementModQ> =
    Arb.byteArray(Arb.constant(ctx.MAX_BYTES_Q), Arb.byte())
        .map { ctx.binaryToElementModQsafe(it, minimum) }

fun generateElementModQ(context: GroupContext): ElementModQ {
    return context.uIntToElementModQ(Random.nextUInt(134217689.toUInt()))
}

fun generateHashedCiphertext(context: GroupContext): HashedElGamalCiphertext {
    return HashedElGamalCiphertext(generateElementModP(context), "what".encodeToByteArray(), generateUInt256(context), 42)
}

fun generateUInt256(context: GroupContext): UInt256 {
    return generateElementModQ(context).toUInt256safe()
}

fun generateElementModP(context: GroupContext) = context.randomElementModP()


/**
 * Property-based testing can run slowly. This will speed things up by turning off shrinking and
 * using fewer iterations. Typical usage:
 * ```
 * forAll(propTestFastConfig, Arb.x(), Arb.y()) { x, y -> ... }
 * ```
 */
@OptIn(ExperimentalKotest::class)
val propTestFastConfig =
    PropTestConfig(maxFailure = 1, shrinkingMode = ShrinkingMode.Off, iterations = 10)

