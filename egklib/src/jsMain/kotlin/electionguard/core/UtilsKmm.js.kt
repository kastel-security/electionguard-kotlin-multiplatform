package electionguard.core

import kotlin.js.Date

/** Get the current time in msecs since epoch */
actual fun getSystemTimeInMillis() = Date().getMilliseconds().toLong()

/** Determine endianness of machine. */
actual fun isBigEndian(): Boolean {
    TODO("Not yet implemented")
}