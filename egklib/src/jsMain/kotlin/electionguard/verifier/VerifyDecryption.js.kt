package electionguard.verifier

import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.ManifestIF
import electionguard.core.ElGamalPublicKey
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.util.ErrorMessages
import electionguard.util.Stats

actual class VerifyDecryption actual constructor(
    group: GroupContext,
    manifest: ManifestIF,
    publicKey: ElGamalPublicKey,
    extendedBaseHash: UInt256
) {
    actual fun verify(
        decrypted: DecryptedTallyOrBallot,
        isBallot: Boolean,
        errs: ErrorMessages,
        stats: Stats
    ): Boolean {
        TODO("Not yet implemented")
    }

    actual fun verifySpoiledBallotTallies(
        ballots: Iterable<DecryptedTallyOrBallot>,
        nthreads: Int,
        errs: ErrorMessages,
        stats: Stats,
        showTime: Boolean
    ): Boolean {
        TODO("Not yet implemented")
    }

}