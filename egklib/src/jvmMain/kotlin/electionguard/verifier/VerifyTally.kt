package electionguard.verifier

import electionguard.ballot.EncryptedBallot
import electionguard.ballot.EncryptedTally
import electionguard.core.*
import electionguard.util.ErrorMessages

/**
 * Verification 8 (Correctness of ballot aggregation)
 * An election verifier must confirm for each (non-placeholder) option in each contest in the election
 * manifest that the aggregate encryption (A, B) satisfies
 *   (8.A) A = Prod(αj) mod p,
 *   (8.B) B = Prod(βj) mod p,
 * where the (αj, βj ) are the corresponding encryptions on all cast ballots in the election record.
 *
 * (10.F) For each contest text label that occurs in at least one submitted ballot, that contest text
 *   label occurs in the list of contests in the corresponding tally.
 */
class VerifyTally(
    val group: GroupContext,
    val aggregator: SelectionAggregator,
) {

    fun verify(encryptedTally: EncryptedTally, errs: ErrorMessages, showTime: Boolean = false): Boolean {
        val starting = getSystemTimeInMillis()

        var nselections = 0
        for (contest in encryptedTally.contests) {
            for (selection in contest.selections) {
                nselections++
                val key: String = contest.contestId + "." + selection.selectionId

                // Already did the accumulation, just have to verify it.
                val accum = aggregator.getAggregateFor(key)
                if (accum != null) {
                    if (selection.encryptedVote.pad != accum.pad) {
                        errs.add("  8.A  Ballot Aggregation does not match: $key")
                    }
                    if (selection.encryptedVote.data != accum.data) {
                        errs.add("  8.B  Ballot Aggregation does not match: $key")
                    }
                } /* else {
                    // TODO what is it? is it needed? left over from placeholders ??
                    if (selection.encryptedVote.pad != group.ZERO_MOD_P || selection.encryptedVote.data != group.ZERO_MOD_P) {
                        errs.add("    Ballot Aggregation empty does not match $key")
                    }
                } */
            }
        }

        // (10.E) For each contest text label that occurs in at least one submitted ballot, that contest text
        // label occurs in the list of contests in the corresponding tally..
        aggregator.contestIdSet.forEach { contestId ->
            if (null == encryptedTally.contests.find { it.contestId == contestId}) {
                errs.add("   10.E Contest '$contestId' found in cast ballots not found in tally")
            }
        }

        val took = getSystemTimeInMillis() - starting
        if (showTime) println("   VerifyAggregation took $took millisecs")

        return !errs.hasErrors()
    }
}

// while we are traversing the encrypted ballots, also accumulate ElGamalCiphertext in order to test the EncryptedTally
// this is bounded by total unique "contestId.selectionId", does not grow by number of ballots
class SelectionAggregator {
    var selectionEncryptions = mutableMapOf<String, ElGamalCiphertext>() // key "contestId.selectionId"
    var contestIdSet = mutableSetOf<String>()
    var nballotsCast = 0

    fun add(ballot: EncryptedBallot) {
        if (ballot.state == EncryptedBallot.BallotState.CAST) {
            nballotsCast++
            for (contest in ballot.contests) {
                contestIdSet.add(contest.contestId)
                for (selection in contest.selections) {
                    val key = "${contest.contestId}.${selection.selectionId}"
                    val total = selectionEncryptions[key]
                    if (total != null) {
                        selectionEncryptions[key] = total + selection.encryptedVote
                    } else {
                        selectionEncryptions[key] = selection.encryptedVote
                    }
                }
            }
        }
    }

    // key "contestId.selectionId"
    fun getAggregateFor(key: String): ElGamalCiphertext? {
        return selectionEncryptions[key]
    }
}