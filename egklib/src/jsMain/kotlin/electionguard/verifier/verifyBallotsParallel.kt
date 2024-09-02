package electionguard.verifier

import electionguard.ballot.EncryptedBallot
import electionguard.util.ErrorMessages
import electionguard.util.Stats

/** Can be multithreaded. */
actual fun VerifyEncryptedBallots.verifyBallotsParallel(
    ballots: Iterable<EncryptedBallot>,
    stats: Stats,
    errs: ErrorMessages
) {
    TODO("Not implemented")
}