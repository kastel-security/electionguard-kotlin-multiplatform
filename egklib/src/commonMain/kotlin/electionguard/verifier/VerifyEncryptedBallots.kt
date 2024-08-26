package electionguard.verifier

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.unwrap
import electionguard.ballot.ElectionConfig
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.EncryptedBallotChain
import electionguard.ballot.ManifestIF
import electionguard.core.*
import electionguard.publish.ElectionRecord
import electionguard.util.ErrorMessages
import electionguard.util.Stats

private const val debugBallots = false

/** Can be multithreaded. */
expect class VerifyEncryptedBallots(
    group: GroupContext,
    manifest: ManifestIF,
    jointPublicKey: ElGamalPublicKey,
    extendedBaseHash: UInt256, // He
    config: ElectionConfig,
    nthreads: Int,
) {
    val aggregator: SelectionAggregator
    val group: GroupContext
    val manifest: ManifestIF
    val jointPublicKey: ElGamalPublicKey
    val extendedBaseHash: UInt256
    val config: ElectionConfig

    fun verifyBallots(
        ballots: Iterable<EncryptedBallot>,
        errs: ErrorMessages,
        stats: Stats = Stats(),
        showTime: Boolean = false
    ): Boolean

    fun verifyEncryptedBallot(
        ballot: EncryptedBallot,
        errs: ErrorMessages,
        stats: Stats
    ) : Boolean

    fun verifyEncryptedContest(
        contest: EncryptedBallot.Contest,
        isPreencrypt: Boolean,
        errs: ErrorMessages
    )

    //////////////////////////////////////////////////////////////////////////////
    // ballot chaining, section 7

    fun verifyConfirmationChain(consumer: ElectionRecord, errs: ErrorMessages): Boolean
}

// check confirmation codes
data class ConfirmationCode(val ballotId: String, val code: UInt256)