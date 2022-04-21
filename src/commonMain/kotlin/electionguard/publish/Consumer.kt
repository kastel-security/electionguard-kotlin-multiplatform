package electionguard.publish

import electionguard.ballot.ElectionRecord
import electionguard.ballot.ElectionRecordAllData
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.PlaintextTally
import electionguard.ballot.SubmittedBallot
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeIF

expect class Consumer(topDir: String, groupContext: GroupContext) {
    fun readElectionRecordAllData(): ElectionRecordAllData
    fun readElectionRecord(): ElectionRecord

    // Use iterators, so that we never have to read in all objects at once.
    fun iteratePlaintextBallots(ballotDir : String, filter : (PlaintextBallot) -> Boolean): Iterable<PlaintextBallot>
    fun iterateSubmittedBallots(): Iterable<SubmittedBallot>
    fun iterateCastBallots(): Iterable<SubmittedBallot>
    fun iterateSpoiledBallots(): Iterable<SubmittedBallot>
    fun iterateSpoiledBallotTallies(): Iterable<PlaintextTally>

    // not part of the election record, private data
    fun readTrustees(trusteeDir: String): List<DecryptingTrusteeIF>
}