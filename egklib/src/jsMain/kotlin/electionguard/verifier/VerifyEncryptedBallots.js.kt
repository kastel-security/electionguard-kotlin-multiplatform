package electionguard.verifier

import electionguard.ballot.ElectionConfig
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.ManifestIF
import electionguard.core.ElGamalPublicKey
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.publish.ElectionRecord
import electionguard.util.ErrorMessages
import electionguard.util.Stats

actual class VerifyEncryptedBallots actual constructor(
    actual val group: GroupContext,
    actual val manifest: ManifestIF,
    actual val jointPublicKey: ElGamalPublicKey,
    actual val extendedBaseHash: UInt256,
    actual val config: ElectionConfig,
    nthreads: Int
) {
    actual fun verifyBallots(
        ballots: Iterable<EncryptedBallot>,
        errs: ErrorMessages,
        stats: Stats,
        showTime: Boolean
    ): Boolean {
        TODO("Not yet implemented")
    }

    actual fun verifyEncryptedBallot(
        ballot: EncryptedBallot,
        errs: ErrorMessages,
        stats: Stats
    ): Boolean {
        TODO("Not yet implemented")
    }

    actual fun verifyEncryptedContest(
        contest: EncryptedBallot.Contest,
        isPreencrypt: Boolean,
        errs: ErrorMessages
    ) {
    }

    actual fun verifyConfirmationChain(
        consumer: ElectionRecord,
        errs: ErrorMessages
    ): Boolean {
        TODO("Not yet implemented")
    }

    actual val aggregator: SelectionAggregator
        get() = TODO("Not yet implemented")

}