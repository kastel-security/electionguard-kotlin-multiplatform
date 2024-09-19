package electionguard.util

import electionguard.core.toByteArray
import kotlin.js.Date

/** Get the current time in msecs since epoch */
actual fun getSystemTimeInMillis() = Date().getMilliseconds().toLong()

/** Determine endianness of machine. */
actual fun isBigEndian(): Boolean {
    //Simple test for endianness of ByteArray
    val testArray = 0x1122.toByteArray()
    return testArray[0] == 0x11.toByte()
}
