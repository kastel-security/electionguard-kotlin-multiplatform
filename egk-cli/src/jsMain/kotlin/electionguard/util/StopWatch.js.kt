package electionguard.util

import electionguard.core.BigInteger
import electionguard.core.normalize
import electionguard.core.toLong

actual fun nanoTime(): Long {
    val process = require("process")
    return process.hrtime.bigint().unsafeCast<BigInteger>()
        .toByteArray().normalize(64).toLong()
}
