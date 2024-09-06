package electionguard.util

/** Get the current time in msecs since epoch */
expect fun getSystemTimeInMillis(): Long

/** Determine endianness of machine. */
expect fun isBigEndian(): Boolean
