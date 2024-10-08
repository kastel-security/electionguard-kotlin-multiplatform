package electionguard.core

import electionguard.runTest
import kotlinx.coroutines.test.TestResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@OptIn(ExperimentalUnsignedTypes::class)
class PowRadixTest {
    @Test
    fun bitSlicingSimplePattern(): TestResult {
        return runTest {
            val testBytes = ByteArray(32) { 0x8F.toByte() }
            val expectedSliceSmall = UShortArray(32) { (0x8F).toUShort() }

            assertContentEquals(
                expectedSliceSmall,
                testBytes.kBitsPerSlice(PowRadixOption.LOW_MEMORY_USE, 32)
            )

            val expectedSliceExtreme = UShortArray(16) { 0x8F8F.toUShort() }

            assertContentEquals(
                expectedSliceExtreme,
                testBytes.kBitsPerSlice(PowRadixOption.EXTREME_MEMORY_USE, 16)
            )

            val expectedSliceLarge =
                UShortArray(22) {
                    if (it == 21) {
                        0x8.toUShort()
                    } else if (it % 2 == 0) {
                        0xF8F.toUShort()
                    } else {
                        0x8F8.toUShort()
                    }
                }

            assertContentEquals(
                expectedSliceLarge,
                testBytes.kBitsPerSlice(PowRadixOption.HIGH_MEMORY_USE, 22)
            )
        }
    }

    @Test
    fun bitSlicingIncreasing(): TestResult {
        // most significant bits are at testBytes[0], which will start off with value
        // one and then increase on our way through the array
        return runTest {
            val testBytes = ByteArray(32) { (it + 1).toByte() }
            val expectedSliceSmall = UShortArray(32) { (32 - it).toUShort() }

            assertContentEquals(
                expectedSliceSmall,
                testBytes.kBitsPerSlice(PowRadixOption.LOW_MEMORY_USE, 32)
            )

            val expectedSliceExtreme =
                UShortArray(16) {
                    val n = 32 - 2 * it - 2 + 1
                    ((n shl 8) or (n + 1)).toUShort()
                }

            assertContentEquals(
                expectedSliceExtreme,
                testBytes.kBitsPerSlice(PowRadixOption.EXTREME_MEMORY_USE, 16)
            )
        }
    }

    @Test
    fun bitSlicingBasics(): TestResult {
        return runTest {
            val option = PowRadixOption.LOW_MEMORY_USE
            val ctx = productionGroup(option)
            val g = ctx.G_MOD_P
            val powRadix = PowRadix(g, option)

            val bytes = 258.toElementModQ(ctx).byteArray()
            // validate it's big-endian
            assertEquals(1, bytes[bytes.size - 2])
            assertEquals(2, bytes[bytes.size - 1])

            val slices = bytes.kBitsPerSlice(option, powRadix.tableLength)
            // validate it's little-endian
            assertEquals(2.toUShort(), slices[0])
            assertEquals(1.toUShort(), slices[1])
            assertEquals(0.toUShort(), slices[2])
        }
    }
}
