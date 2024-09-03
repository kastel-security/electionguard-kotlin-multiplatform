package electionguard.core

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.ByteOrder
import java.nio.ByteOrder.nativeOrder
import java.nio.file.Files
import java.nio.file.Path

private val logger = KotlinLogging.logger("UtilsKmmJvm")

actual fun getSystemTimeInMillis() : Long = System.currentTimeMillis()

fun pathExists(path: String): Boolean = Files.exists(Path.of(path))

fun createDirectories(directory: String): Boolean {
    if (pathExists(directory)) {
        return true
    }
    return try {
        Files.createDirectories(Path.of(directory))
        logger.warn { "error createDirectories = '$directory' " }
        true
    } catch (t: Throwable) {
        false
    }
}

fun isDirectory(path: String): Boolean = Files.isDirectory(Path.of(path))

fun fileReadLines(filename: String): List<String> = File(filename).readLines()

fun fileReadBytes(filename: String): ByteArray = File(filename).readBytes()

fun fileReadText(filename: String): String = File(filename).readText()

actual fun isBigEndian(): Boolean = nativeOrder() == ByteOrder.BIG_ENDIAN





