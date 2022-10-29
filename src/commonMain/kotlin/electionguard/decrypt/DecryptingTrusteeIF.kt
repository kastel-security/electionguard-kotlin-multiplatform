package electionguard.decrypt

import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext

/** The interface to a DecryptingTrustee. */
interface DecryptingTrusteeIF {
    /** Guardian id.  */
    fun id(): String

    /** Guardian x coordinate, for compensated partial decryption  */
    fun xCoordinate(): Int

    /** The guardian's public key = K_i.  */
    fun electionPublicKey(): ElementModP

    /**
     * Set missing Guardian info. Once set, cannot be changed.
     * @param lagrangeCoeff    the lagrange coefficient for this trustee
     * @param missingGuardians the missing guardians' Ids
     */
    fun setMissing(
        group: GroupContext,
        lagrangeCoeff: ElementModQ,
        missingGuardians: List<String>,
    ) : Boolean

    /**
     * Compute partial decryption(s) of elgamal encryption(s), using spec 1.52 eq 58 and 59.
     *
     * @param texts            list of ElementModP that will be partially decrypted
     * @param nonce            an optional nonce to generate the proof
     * @return a list of partial decryptions, in the same order as the texts
     */
    fun decrypt(
        group: GroupContext,
        texts: List<ElementModP>,
        nonce: ElementModQ?, // LOOK do we need this?
    ): List<PartialDecryption>

    /**
     * Compute responses to Chaum-Pedersen challenges
     * @param challenges            list of Chaum-Pedersen challenges
     * @return a list of responses, in the same order as the challenges
     */
    fun challenge(
        group: GroupContext,
        challenges: List<ChallengeRequest>,
    ): List<ChallengeResponse>
}