package electionguard.util

import java.nio.ByteOrder
import java.nio.ByteOrder.nativeOrder

/** Get the current time in msecs since epoch */
actual fun getSystemTimeInMillis() : Long = System.currentTimeMillis()

/** Determine endianness of machine. */
actual fun isBigEndian(): Boolean = nativeOrder() == ByteOrder.BIG_ENDIAN
