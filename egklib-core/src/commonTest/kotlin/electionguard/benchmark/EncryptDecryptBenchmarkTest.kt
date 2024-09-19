package electionguard.benchmark

import electionguard.core.*
import electionguard.getTestPlatform
import electionguard.runTest
import io.kotest.property.checkAll
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.measureTime

class EncryptDecryptBenchmarkTest {

    @Test
    @Ignore
    fun benchmarkEncryptDecryptForJavaScriptTargets() = runTest {
        listOf(
            PowRadixOption.NO_ACCELERATION,
            PowRadixOption.LOW_MEMORY_USE,
            PowRadixOption.HIGH_MEMORY_USE,
            PowRadixOption.HIGH_MEMORY_USE
        ).forEach { powRadixOption ->
            val group = productionGroup(powRadixOption)
            println("Benchmarking EncryptDecrypt with $group for Target: ${getTestPlatform()}")
            var count = 0
            var totalTimeEncrypt: Duration = Duration.ZERO
            var totalTimeDecrypt: Duration = Duration.ZERO
            checkAll(
                propTestFastConfig,
                elGamalKeypairs(group),
                elementsModQNoZero(group),
                smallInts()
            ) { keypair, nonce, message ->
                val encrypted: ElGamalCiphertext
                measureTime { encrypted = message.encrypt(keypair, nonce) }
                    .also { totalTimeEncrypt += it }
                val decrypted: Int?
                measureTime {
                    decrypted = encrypted.decrypt(keypair)
                }.also { totalTimeDecrypt += it }
                count++
                assertEquals(message, decrypted)
            }
            println("Total time for $count iterations: ${totalTimeEncrypt + totalTimeDecrypt}")
            println("Average time per encryption: ${totalTimeEncrypt / count}")
            println("Average time per decryption: ${totalTimeDecrypt / count}")
        }
    }
}
