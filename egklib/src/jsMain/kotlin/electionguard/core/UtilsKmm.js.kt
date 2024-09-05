package electionguard.core

import electionguard.util.jsObject
import node.buffer.BufferEncoding
import node.fs.existsSync
import node.fs.mkdirSync
import node.fs.readFileSync
import node.fs.readdirSync
import kotlin.js.Date

/** Get the current time in msecs since epoch */
actual fun getSystemTimeInMillis() = Date().getMilliseconds().toLong()

//All file system operations are only available when targeting NodeJs
/** Does this path exist? */
actual fun pathExists(path: String): Boolean {
    return existsSync(path)
}

/** Create the named directory */
actual fun createDirectories(directory: String): Boolean = runCatching {
    mkdirSync(directory, jsObject { recursive = true } as Any)
}.also {
    println(it)
}.isFailure


/** Is this path a directory? */
actual fun isDirectory(path: String): Boolean {
    return try {
        readdirSync(path, options = null as BufferEncoding?)
        true
    } catch (t: Throwable) {
        false
    }
}

/** Read lines from a file. */
actual fun fileReadLines(filename: String): List<String> {
    return fileReadText(filename).split("[\r\n|\n]")
}

/** Read all the bytes in a file. */
actual fun fileReadBytes(filename: String): ByteArray {
    return fileReadText(filename).encodeToByteArray()
}

/** Read all int text in a file. */
actual fun fileReadText(filename: String): String {
    return readFileSync(filename, BufferEncoding.utf8)
}

/** Determine endianness of machine. */
actual fun isBigEndian(): Boolean {
    //Simple test for endianness of ByteArray
    val testArray = 0x1122.toByteArray()
    return testArray[0] == 0x11.toByte()
}