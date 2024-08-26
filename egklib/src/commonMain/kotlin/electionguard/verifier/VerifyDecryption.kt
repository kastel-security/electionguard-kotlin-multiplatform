package electionguard.verifier

import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.ManifestIF
import electionguard.core.*
import electionguard.util.ErrorMessages
import electionguard.util.Stats

/** Box [9, 10, 11] (tally), and [12, 13, 14] (ballot). can be multithreaded. */
// Note that 12,13,14 (ballot) are almost the same as 9,10,11 (tally). Only diff is 13.B,C
expect class VerifyDecryption(
    group: GroupContext,
    manifest: ManifestIF,
    publicKey: ElGamalPublicKey,
    extendedBaseHash: UInt256,
) {

    fun verify(decrypted: DecryptedTallyOrBallot, isBallot: Boolean, errs: ErrorMessages, stats: Stats): Boolean

    fun verifySpoiledBallotTallies(
        ballots: Iterable<DecryptedTallyOrBallot>,
        nthreads: Int,
        errs: ErrorMessages,
        stats: Stats,
        showTime: Boolean,
    ): Boolean

}