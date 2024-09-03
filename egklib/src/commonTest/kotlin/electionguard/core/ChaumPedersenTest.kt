package electionguard.core

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.math.max
import kotlin.math.min
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChaumPedersenTest {
    @Test
    fun testCCKnownNonceProofsSimpleEncryptionZero() {
        runTest {
            val context = tinyGroup()
            val keypair = elGamalKeyPairFromSecret(context.TWO_MOD_Q)
            val nonce = context.TWO_MOD_Q
            val message0 = 0.encrypt(keypair, nonce)
            val message1 = 1.encrypt(keypair, nonce)
            val extendedBaseHash = UInt256.ONE
            val extendedBaseHash2 = UInt256.TWO

            val goodProof =
                message0.makeChaumPedersen(
                    0,
                    1,
                    nonce,
                    keypair.publicKey,
                    extendedBaseHash
                )
            println(goodProof.verify(message0, keypair.publicKey, extendedBaseHash, 1))
            assertTrue(goodProof.verify(message0, keypair.publicKey, extendedBaseHash, 1) is Ok)

            println(goodProof.verify(message0, keypair.publicKey, extendedBaseHash2, 1)) // extendedBaseHash doesnt match
            assertFalse(goodProof.verify(message0, keypair.publicKey, extendedBaseHash2, 1) is Ok)

            val badProof1 =
                message0.makeChaumPedersen(
                    1,  // vote doesnt match
                    1,
                    nonce,
                    keypair.publicKey,
                    extendedBaseHash
                )
            println(badProof1.verify(message0, keypair.publicKey, extendedBaseHash, 1))
            assertFalse(badProof1.verify(message0, keypair.publicKey, extendedBaseHash, 1) is Ok)

            val badProof2 =
                message1.makeChaumPedersen(
                    0,  // vote doesnt match
                    1,
                    nonce,
                    keypair.publicKey,
                    extendedBaseHash
                )
            println(badProof2.verify(message0, keypair.publicKey, extendedBaseHash, 1)) // vote doesnt match
            assertFalse(badProof2.verify(message0, keypair.publicKey, extendedBaseHash, 1) is Ok)

            // validating equivalent but different message (same nonce)
            val differentMessage = 0.encrypt(keypair, nonce)
            val badProof3 =
                differentMessage.makeChaumPedersen(
                    0,
                    1,
                    nonce,
                    keypair.publicKey,
                    extendedBaseHash
                )
            println(badProof3.verify(message0, keypair.publicKey, extendedBaseHash, 1)) // new
            assertTrue(badProof3.verify(message0, keypair.publicKey, extendedBaseHash, 1) is Ok)
        }
    }

    @Test
    @Ignore // causes the browser tests to crash
    fun testCCProofsKnownNonce() {
        runTest {
            val context = tinyGroup()
            checkAll(
                elGamalKeypairs(context),
                elementsModQNoZero(context),
                Arb.int(min=0, max=100),
                Arb.int(min=0, max=100)
            ) { keypair, nonce, constant, constant2 ->
                // we need the constants to be different
                val badConstant = if (constant == constant2) constant + 1 else constant2

                val message = constant.encrypt(keypair, nonce)
                val badMessage = badConstant.encrypt(keypair, nonce)

                val proof =
                    message.makeChaumPedersen(
                        vote = constant,
                        limit = constant,
                        nonce = nonce,
                        publicKey = keypair.publicKey,
                        extendedBaseHash = UInt256.ONE
                    )
                assertTrue(
                    proof.verify(
                        message,
                        keypair.publicKey,
                        UInt256.ONE,
                        limit = constant
                    )  is Ok,
                    "first proof is valid"
                )
                assertFalse(
                    proof.verify(
                        message,
                        keypair.publicKey,
                        UInt256.ONE,
                        limit = constant + 1
                    ) is Ok,
                    "modified constant invalidates proof"
                )
                assertFalse(
                    proof.verify(badMessage, keypair.publicKey, UInt256.ONE, 1) is Ok,
                    "modified message invalidates proof"
                )

                val badProof =
                    badMessage.makeChaumPedersen(
                        vote = constant,
                        limit = constant,
                        nonce = nonce,
                        publicKey = keypair.publicKey,
                        extendedBaseHash = UInt256.ONE
                    )
                assertFalse(
                    badProof.verify(badMessage, keypair.publicKey, UInt256.ONE, 1) is Ok,
                    "modified proof with consistent message is invalid"
                )

                val badProof2 =
                    message.makeChaumPedersen(
                        vote = badConstant,
                        limit = badConstant,
                        nonce = nonce,
                        publicKey = keypair.publicKey,
                        extendedBaseHash = UInt256.ONE
                    )
                assertFalse(
                    badProof2.verify(message, keypair.publicKey, UInt256.ONE, 1) is Ok,
                    "modified proof with inconsistent message is invalid"
                )

                //                val badProof3 = proof.copy(constant = Int.MAX_VALUE)
                //                assertFalse(badProof3.isValid(message, keypair.publicKey,
                // ONE_MOD_Q))
            }
        }
    }

    @Test
    fun testDisjunctiveProofKnownNonceSimple() {
        runTest {
            val context = tinyGroup()
            val keypair = elGamalKeyPairFromSecret(context.TWO_MOD_Q)
            val nonce = context.ONE_MOD_Q
            val message0 = 0.encrypt(keypair, nonce)
            val message1 = 1.encrypt(keypair, nonce)
            val badMessage0 = 0.encrypt(keypair, context.TWO_MOD_Q)
            val badMessage1 = 1.encrypt(keypair, context.TWO_MOD_Q)

            val hashHeader = UInt256.ONE
            val badHashHeader = UInt256.TWO
            val goodProof0 =
                message0.makeChaumPedersen(
                    0,
                    1,
                    nonce,
                    keypair.publicKey,
                    hashHeader
                )
            val goodProof1 =
                message1.makeChaumPedersen(
                    1,
                    1,
                    nonce,
                    keypair.publicKey,
                    hashHeader
                )
            val badProof0 =
                message0.makeChaumPedersen(
                    1,
                    1,
                    nonce,
                    keypair.publicKey,
                    hashHeader
                )
            val badProof1 =
                message1.makeChaumPedersen(
                    0,
                    1,
                    nonce,
                    keypair.publicKey,
                    hashHeader
                )
            val badProof2 =
                badMessage0.makeChaumPedersen(
                    0,
                    1,
                    nonce,
                    keypair.publicKey,
                    hashHeader
                )
            val badProof3 =
                badMessage1.makeChaumPedersen(
                    0,
                    1,
                    nonce,
                    keypair.publicKey,
                    hashHeader
                )

            assertTrue(goodProof0.verify(message0, keypair.publicKey, hashHeader, 1) is Ok)
            assertTrue(goodProof1.verify(message1, keypair.publicKey, hashHeader, 1) is Ok)

            assertFalse(goodProof0.verify(message1, keypair.publicKey, hashHeader, 1) is Ok)
            assertFalse(goodProof0.verify(message1, keypair.publicKey, hashHeader, 1) is Ok)

            assertFalse(goodProof1.verify(message0, keypair.publicKey, badHashHeader, 1) is Ok)
            assertFalse(goodProof1.verify(message0, keypair.publicKey, badHashHeader, 1) is Ok)

            assertFalse(badProof0.verify(message0, keypair.publicKey, hashHeader, 1) is Ok)
            assertFalse(badProof0.verify(message1, keypair.publicKey, hashHeader, 1) is Ok)

            assertFalse(badProof1.verify(message0, keypair.publicKey, hashHeader, 1) is Ok)
            assertFalse(badProof1.verify(message1, keypair.publicKey, hashHeader, 1) is Ok)

            assertFalse(badProof2.verify(message0, keypair.publicKey, hashHeader, 1) is Ok)
            assertFalse(badProof2.verify(message1, keypair.publicKey, hashHeader, 1) is Ok)

            assertFalse(badProof3.verify(message1, keypair.publicKey, hashHeader, 1) is Ok)
            assertFalse(badProof3.verify(message0, keypair.publicKey, hashHeader, 1) is Ok)
        }
    }

    @Test
    fun disjunctiveProofs() {
        runTest {
            val context = tinyGroup()
            checkAll(
                Arb.int(min=0, max=1),
                elementsModQNoZero(context),
                elGamalKeypairs(context),
                uint256s(),
            ) { constant, nonce, keypair, hashHeader ->
                val ciphertext = constant.encrypt(keypair, nonce)
                val proof =
                    ciphertext.makeChaumPedersen(
                        constant,
                        1,
                        nonce,
                        keypair.publicKey,
                        hashHeader
                    )

                assertTrue(proof.verify(ciphertext, keypair.publicKey, hashHeader, 1) is Ok)

                // now, swap the proofs around and verify it fails
                val mutated = listOf(proof.proofs[1], proof.proofs[0], proof.proofs[1])
                val badProof = proof.copy(proofs = mutated)

                assertFalse(badProof.verify(ciphertext, keypair.publicKey, hashHeader, 1) is Ok)
            }
        }
    }

    @Test
    fun testAccum() {
        runTest {
            val context = productionGroup()
            val constant = 42
            val key = context.ONE_MOD_P
            val nonce = context.TWO_MOD_Q
            val hashHeader = UInt256.ONE
            val publicKey = ElGamalPublicKey(key)

            val vote0 = 0.encrypt(publicKey, nonce)
            val vote1 = 1.encrypt(publicKey, nonce)
            val vote41 = 41.encrypt(publicKey, nonce)
            val nonceAccum = nonce + nonce + nonce // we're using it three times

            val texts: List<ElGamalCiphertext> = listOf(vote0, vote1, vote41)
            val message: ElGamalCiphertext = texts.encryptedSum()?: 0.encrypt(publicKey)

            val proof =
                message.makeChaumPedersen(
                    vote = constant,
                    limit = constant,
                    nonce = nonceAccum,
                    publicKey = publicKey,
                    extendedBaseHash = hashHeader
                )

            assertTrue(
                proof.verify(
                    message,
                    publicKey = publicKey,
                    extendedBaseHash = hashHeader,
                    limit = constant
                ) is Ok,
                "proof not valid"
            )
        }
    }

    @Test
    fun testAccumDifferentNonces() {
        runTest {
            val context = productionGroup()
            val constant = 42
            val contestNonce = context.randomElementModQ(2)
            val hashHeader = UInt256.random()
            val keyPair = elGamalKeyPairFromSecret(context.randomElementModQ(2))
            val publicKey = keyPair.publicKey

            val randomQ = context.randomElementModQ(2)
            val nonceSequence = Nonces(randomQ, contestNonce)

            val vote0 = 0.encrypt(publicKey, nonceSequence[0])
            val vote1 = 1.encrypt(publicKey, nonceSequence[1])
            val vote41 = 41.encrypt(publicKey, nonceSequence[2])
            val nonceAccum = nonceSequence[0] + nonceSequence[1] + nonceSequence[2]

            val texts: List<ElGamalCiphertext> = listOf(vote0, vote1, vote41)
            val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()?: 0.encrypt(publicKey)

            val proof =
                ciphertextAccumulation.makeChaumPedersen(
                    vote = constant,
                    limit = constant,
                    nonce = nonceAccum,
                    publicKey = publicKey,
                    extendedBaseHash = hashHeader
                )

            assertTrue(
                proof.verify(
                    ciphertextAccumulation,
                    publicKey = publicKey,
                    extendedBaseHash = hashHeader,
                    limit = constant
                ) is Ok,
                "proof not valid"
            )
        }
    }

    @Test
    fun testRangeProofsSimple() {
        val context = tinyGroup()
        val secretKey = 2.toElementModQ(context)
        val keypair = elGamalKeyPairFromSecret(secretKey)
        val publicKey = keypair.publicKey
        val nonce = 3.toElementModQ(context)
        val qbar = UInt256.random()
        val ciphertext0 = 0.encrypt(keypair, nonce)
        val ciphertext2 = 2.encrypt(keypair, nonce)

        val rangeProof01 = ciphertext0.makeChaumPedersen(0, 1, nonce, publicKey,  qbar)
        val valid01 = rangeProof01.verify(ciphertext0, publicKey, qbar, 1)

        val rangeProof02 = ciphertext0.makeChaumPedersen(0, 2, nonce, publicKey,  qbar)
        val valid02 = rangeProof02.verify(ciphertext0, publicKey, qbar, 2)

        val rangeProof22 = ciphertext2.makeChaumPedersen(2, 2, nonce, publicKey, qbar)
        val valid22 = rangeProof22.verify(ciphertext2, publicKey, qbar, 2)

        if (valid01 is Err) {
            println("Error in valid01: $valid01")
        }

        if (valid02 is Err) {
            println("Error in valid02: $valid02")
        }

        if (valid22 is Err) {
            println("Error in valid02: $valid22")
        }

        assertTrue(valid01 is Ok && valid02 is Ok && valid22 is Ok)
    }

    @Test
    fun testRangeProofs() {
        val context = tinyGroup()
        runTest {
            checkAll(
                Arb.int(0, 5),
                Arb.int(0, 5),
                elementsModQNoZero(context),
                elGamalKeypairs(context),
                uint256s(),
            ) { p0, p1, nonce, keypair, qbar ->
                val plaintext = min(p0, p1)
                val rangeLimit = max(p0, p1)

                val ciphertext = plaintext.encrypt(keypair, nonce)
                val proof = ciphertext.makeChaumPedersen(
                    plaintext,
                    rangeLimit,
                    nonce,
                    keypair.publicKey,
                    qbar
                )

                val proofValidation = proof.verify(ciphertext, keypair.publicKey, qbar, rangeLimit)
                assertTrue(proofValidation is Ok)
            }
        }
    }

    @Test
    fun testBadRangeProofs() {
        val context = tinyGroup()
        runTest {
            checkAll(
                propTestFastConfig,
                Arb.int(0, 5),
                Arb.int(1, 5),
                elementsModQNoZero(context),
                elGamalKeypairs(context),
                uint256s(),
            ) { p0, p1, nonce, keypair, qbar ->
                // we're deliberately making the plaintext greater than the range limit!
                val plaintext = p0 + p1

                val ciphertext = plaintext.encrypt(keypair, nonce)
                val proof = ciphertext.makeChaumPedersen(
                    plaintext,
                    p0,
                    nonce,
                    keypair.publicKey,
                    qbar,
                    true // disables checks that would otherwise reject this proof
                )

                val proofValidation = proof.verify(ciphertext, keypair.publicKey, qbar, p0)
                assertTrue(proofValidation is Err)
                println("testBadRangeProofs $p0, $p1 = ${proofValidation.error}")
            }
        }
    }
}