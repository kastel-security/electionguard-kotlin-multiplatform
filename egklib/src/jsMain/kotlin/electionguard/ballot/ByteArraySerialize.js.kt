package electionguard.ballot

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.core.toByteArray
import electionguard.core.toInt

actual fun ContestData.encodeToByteArray(fill: String?): ByteArray {
    return this.overvotes.size.toByteArray() +
        this.overvotes.map { it.toByteArray() }.fold(byteArrayOf()) { a, b -> a + b } +
        this.writeIns.size.toByteArray() +
        this.writeIns.map { encodeString(it) }.fold(byteArrayOf()) { a, b -> a + b } +
        encodeString(this.status.name) +
        if (!fill.isNullOrEmpty()) ByteArray(fill.length) else byteArrayOf()
}

/**
 * Encodes the given string to a ByteArray.
 *
 * This function converts the specified string into its corresponding byte array
 * and then returns a new ByteArray combining the length of the encoded string
 * and the encoded string itself.
 *
 * @param string the string to be encoded
 * @return a ByteArray containing the length of the encoded string followed by
 * the encoded string itself
 */
fun encodeString(string: String): ByteArray {
    val encodedString = string.encodeToByteArray()
    return encodedString.size.toByteArray() + encodedString
}


/**
 * Decodes a byte array into a pair consisting of an integer and a string.
 *
 * @param bytes A ByteArray where the first 32 bytes represent the number of bytes the string has.
 * @return A Pair containing an integer (length of the encoded string) and the decoded string.
 */
fun decodeString(bytes: ByteArray): Pair<Int, String> {
    val length = bytes.copyOfRange(0, 4).toInt() + 4
    return length to bytes.copyOfRange(4, length).decodeToString()
}

actual fun ByteArray.decodeToContestData(): Result<ContestData, String> {
    var currentOffset = 0
    val overVotesSize = this.copyOfRange(0, 4.also { currentOffset += it }).toInt()

    val overVotes = this.copyOfRange(
        currentOffset,
        (currentOffset + overVotesSize * 4).also { currentOffset = it }
    ).toList().chunked(4).map { it.toByteArray().toInt() }

    val writeInsSize = this.copyOfRange(
        currentOffset,
        (currentOffset + 4).also { currentOffset = it }
    ).toInt()
    val writeIns = IntRange(0, writeInsSize - 1).map {
        decodeString(this.copyOfRange(currentOffset, this.size))
            .also { currentOffset += it.first }
            .second
    }

    val status = ContestDataStatus.valueOf(
        decodeString(this.copyOfRange(currentOffset, this.size)).second
    )

    return Ok(ContestData(overVotes, writeIns, status))
}
