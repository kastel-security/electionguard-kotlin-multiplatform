package electionguard.verifier

import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.util.ErrorMessages
import electionguard.util.Stats

actual fun VerifyDecryption.verifySpoiledBallotsParallel(
    ballots: Iterable<DecryptedTallyOrBallot>,
    stats: Stats,
    errs: ErrorMessages,
    nthreads: Int
): Int {
    TODO("Not yet implemented")
}