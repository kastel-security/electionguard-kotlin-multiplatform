package electionguard.verifier

import electionguard.model.DecryptedTallyOrBallot
import electionguard.model.ManifestIF
import electionguard.core.ElGamalPublicKey
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.core.toElementModQ
import electionguard.core.verifyContestDataDecryption
import electionguard.core.verifyDecryption
import electionguard.util.ErrorMessages
import electionguard.util.Stats
import electionguard.util.getSystemTimeInMillis
import kotlin.math.roundToInt

actual class VerifyDecryption actual constructor(
    val group: GroupContext,
    val manifest: ManifestIF,
    val publicKey: ElGamalPublicKey,
    val extendedBaseHash: UInt256
) {
    /** All manifest "contestId/selectionId" strings. */
    val contestAndSelectionSet : Set<String> = manifest.contests.map { contest -> contest.selections.map {
        "${contest.contestId}/${it.selectionId}" } }.flatten().toSet()

    actual fun verify(
        decrypted: DecryptedTallyOrBallot,
        isBallot: Boolean,
        errs: ErrorMessages,
        stats: Stats
    ): Boolean {
        val starting = getSystemTimeInMillis()

        if (decrypted.electionId != extendedBaseHash) {
            errs.add("DecryptedTallyOrBallot has wrong electionId = ${decrypted.electionId}")
            return false
        }

        var nselections = 0

        for (contest in decrypted.contests) {
            val cerrs = errs.nested("Contest ${contest.contestId}")

            // (10.B, 13.D) The contest text label occurs as a contest label in the contests in the election manifest.
            if (manifest.findContest(contest.contestId) == null) {
                val what = if (isBallot) "13.D" else "10.B"
                cerrs.add("    $what contest '${contest.contestId}' not in manifest")
                continue
            }

            val optionLimit = manifest.optionLimit(contest.contestId)
            val contestLimit = manifest.contestLimit(contest.contestId)

            if (contest.decryptedContestData != null) {
                verifyContestData(contest.decryptedContestData!!, errs)
            }

            val contestSelectionSet = mutableSetOf<String>()
            var contestVotes = 0
            for (selection in contest.selections) {
                val serrs = cerrs.nested("Selection ${selection.selectionId}")

                nselections++
                val here = "${contest.contestId}/${selection.selectionId}"
                contestSelectionSet.add(selection.selectionId)

                // (10.C, 13.E) the option text label occurs as an option label for the contest in the election manifest.
                if (!contestAndSelectionSet.contains(here)) {
                    val what = if (isBallot) "13.E" else "10.C"
                    serrs.add("    $what contest/selection '$here' not in manifest")
                    continue
                }

                if (!selection.proof.r.inBounds()) {
                    val what = if (isBallot) "12.A" else "9.A"
                    serrs.add("    $what response out of bounds")
                }

                // 9.B, 11.B
                if (!selection.verifySelection()) {
                    val what = if (isBallot) "12.B" else "9.B"
                    serrs.add("    $what challenge does not match")
                }

                // T = K^t mod p.
                val tallyQ = selection.tally.toElementModQ(group)
                if (selection.bOverM != publicKey powP tallyQ) {
                    if (isBallot) serrs.add("    10.A incorrect Decryption T = K^t mod p")
                    else serrs.add("    13.A incorrect Decryption S = K^sigma mod p")
                }

                if (isBallot && (selection.tally !in (0..optionLimit))) {
                    serrs.add("     13.B ballot vote ${selection.tally} must be between 0..$optionLimit")
                }
                contestVotes += selection.tally
            }

            // (10.D) For each option in the election manifest, the option occurs in the decrypted tally contest.
            // (13.F) For each option in the election manifest, the option occurs in the decrypted ballot contest.
            val mcontest = manifest.findContest(contest.contestId)!! // already checked existence above
            mcontest.selections.forEach {
                if (!contestSelectionSet.contains(it.selectionId)) {
                    if (isBallot) errs.add("    13.F Manifest contains selection '$it' not on decrypted ballot")
                    else errs.add("    10.D Manifest contains selection '$it' not on decrypted tally ")
                }
            }

            if (isBallot) {
                if (contestVotes !in (0..contestLimit)) {
                    cerrs.add("     13.C sum of votes ${contestVotes} in contest must be between 0..$contestLimit")
                }
            }
        }

        stats.of("verifyDecryption", "selections").accum(getSystemTimeInMillis() - starting, nselections)
        return !errs.hasErrors()
    }

    // Verification 9 (Correctness of tally decryptions)
    private fun DecryptedTallyOrBallot.Selection.verifySelection(): Boolean {
        return this.proof.verifyDecryption(extendedBaseHash, publicKey.key, this.encryptedVote, this.bOverM)

    }

    // Verification 11 (Correctness of decryptions of contest data)
    // An election verifier must confirm the correct decryption of the contest data field for each contest by
    // verifying the conditions analogous to Verification 9 for the corresponding NIZK proof with (A, B)
    // replaced by (C0 , C1 , C2 ) and Mi by mi as follows. An election verifier must compute the following values.
    // (11.1) a = g v · K c mod p,
    // (11.2) b = C0v · β c mod p.
    // An election verifier must then confirm the following.
    // (11.A) The given value v is in the set Zq .
    // (11.B) The challenge value c satisfies c = H(HE ; 0x31, K, C0 , C1 , C2 , a, b, β).
    private fun verifyContestData(decryptedContestData: DecryptedTallyOrBallot.DecryptedContestData, errs: ErrorMessages){
        // (11.A,14.A) The given value v is in the set Zq.
        if (!decryptedContestData.proof.r.inBounds()) {
            errs.add("     (11.A,14.A) The value v is not in the set Zq.")
        }
        if (!decryptedContestData.proof.verifyContestDataDecryption(publicKey.key, extendedBaseHash, decryptedContestData.beta, decryptedContestData.encryptedContestData)) {
            errs.add("     (11.B,14.B) The challenge value is wrong")
        }
    }

    actual fun verifySpoiledBallotTallies(
        ballots: Iterable<DecryptedTallyOrBallot>,
        nthreads: Int,
        errs: ErrorMessages,
        stats: Stats,
        showTime: Boolean
    ): Boolean {
        val starting = getSystemTimeInMillis()
        var count = 0
        for (ballot in ballots) {
            verify(ballot, isBallot = true, errs.nested("Ballot ${ballot.id}"), stats)
            count++
        }

        val took = getSystemTimeInMillis() - starting
        val perBallot = if (count == 0) 0 else (took.toDouble() / count).roundToInt()
        if (showTime) println("   verifySpoiledBallotTallies took $took millisecs for $count ballots = $perBallot msecs/ballot wallclock")
        return !errs.hasErrors()
    }

}