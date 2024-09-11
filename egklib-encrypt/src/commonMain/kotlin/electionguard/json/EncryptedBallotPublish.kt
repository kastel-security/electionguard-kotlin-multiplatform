package electionguard.json

import electionguard.core.Base16.toHex
import electionguard.core.toUInt256safe
import electionguard.model.EncryptedBallot
import electionguard.preencrypt.RecordedPreBallot
import electionguard.preencrypt.RecordedPreEncryption
import electionguard.preencrypt.RecordedSelectionVector

fun EncryptedBallot.publishJson(recordedPreBallot: RecordedPreBallot) = EncryptedBallotJson(
    this.ballotId,
    this.ballotStyleId,
    this.encryptingDevice,
    this.timestamp,
    this.codeBaux.toHex(),
    this.confirmationCode.publishJson(),
    this.electionId.publishJson(),
    this.contests.map { it.publishJson(recordedPreBallot) },
    this.state.name,
    this.encryptedSn?.publishJson(),
    true,
    null,
)

private fun EncryptedBallot.Contest.publishJson(recordedPreBallot: RecordedPreBallot): EncryptedContestJson {

    val rcontest = recordedPreBallot.contests.find { it.contestId == this.contestId }
        ?: throw IllegalArgumentException("Cant find ${this.contestId}")

    return EncryptedContestJson(
        this.contestId,
        this.sequenceOrder,
        this.votesAllowed,
        this.contestHash.publishJson(),
        this.selections.map {
            EncryptedSelectionJson(
                it.selectionId,
                it.sequenceOrder,
                it.encryptedVote.publishJson(),
                it.proof.publishJson(),
            )
        },
        this.proof.publishJson(),
        this.contestData.publishJson(),
        rcontest.publishJson(),
    )
}

private fun RecordedPreEncryption.publishJson(): PreEncryptionJson {
    return PreEncryptionJson(
        this.preencryptionHash.publishJson(),
        this.allSelectionHashes.map { it.publishJson() },
        this.selectedVectors.map { it.publishJson() },
    )
}

private fun RecordedSelectionVector.publishJson(): SelectionVectorJson {
    return SelectionVectorJson(
        this.selectionHash.toUInt256safe().publishJson(),
        this.shortCode,
        this.encryptions.map { it.publishJson() },
    )
}
