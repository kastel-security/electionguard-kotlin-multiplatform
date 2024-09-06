package electionguard.util

import node.buffer.BufferEncoding
import node.fs.existsSync
import node.fs.mkdirSync
import node.fs.readFileSync

//All file system operations are only available when targeting NodeJs
/** Does this path exist? */
actual fun pathExists(path: String): Boolean {
    return existsSync(path)
}

/** Create the named directory */
actual fun createDirectories(directory: String): Boolean = runCatching {
    mkdirSync(directory, jsObject { recursive = true } as Any)
}.isSuccess


/** Is this path a directory? */
actual fun isDirectory(path: String) =
    pathExists(path) && require("fs").statSync(path).isDirectory() as Boolean

/** Read lines from a file. */
actual fun fileReadLines(filename: String): List<String> {
    return fileReadText(filename).split("\r\n", "\n")
}

/** Read all the bytes in a file. */
actual fun fileReadBytes(filename: String): ByteArray {
    return fileReadText(filename).encodeToByteArray()
}

/** Read all int text in a file. */
actual fun fileReadText(filename: String): String {
    return readFileSync(filename, BufferEncoding.utf8)
}
