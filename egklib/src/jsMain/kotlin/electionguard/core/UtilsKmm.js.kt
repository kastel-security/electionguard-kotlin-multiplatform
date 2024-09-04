package electionguard.core

import kotlin.js.Date

/** Get the current time in msecs since epoch */
actual fun getSystemTimeInMillis() = Date().getMilliseconds().toLong()

/** Does this path exist? */
actual fun pathExists(path: String): Boolean {
    TODO("Not yet implemented")
}

/** Create the named directory */
actual fun createDirectories(directory: String): Boolean {
    TODO("Not yet implemented")
}

/** Is this path a directory? */
actual fun isDirectory(path: String): Boolean {
    TODO("Not yet implemented")
}

/** Read lines from a file. */
actual fun fileReadLines(filename: String): List<String> {
    TODO("Not yet implemented")
}

/** Read all the bytes in a file. */
actual fun fileReadBytes(filename: String): ByteArray {
    TODO("Not yet implemented")
}

/** Read all int text in a file. */
actual fun fileReadText(filename: String): String {
    TODO("Not yet implemented")
}

/** Determine endianness of machine. */
actual fun isBigEndian(): Boolean {
    //Simple test for endianness of ByteArray
    val testArray = 0x1122.toByteArray()
    return testArray[0] == 0x11.toByte()
}