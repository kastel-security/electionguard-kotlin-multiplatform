package electionguard.keyceremony

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.core.ElGamalSecretKey
import electionguard.core.ElementModP
import electionguard.core.ElementModQ
import electionguard.core.GroupContext
import electionguard.core.SchnorrProof
import electionguard.core.decrypt
import electionguard.core.hashedElGamalEncrypt
import electionguard.core.toElementModQ
import electionguard.core.toUInt256

/**
 * A Trustee that knows its own secret key and polynomial.
 * KeyCeremonyTrustee must stay private. Guardian is its public info in the election record.
 */
class KeyCeremonyTrustee(
    val group: GroupContext,
    val id: String,
    val xCoordinate: Int,
    val quorum: Int,
) : KeyCeremonyTrusteeIF {
    private val polynomial: ElectionPolynomial = group.generatePolynomial(id, quorum)

    // All of the guardians' public keys (including this one), keyed by guardian id.
    val guardianPublicKeys: MutableMap<String, PublicKeys> = mutableMapOf()

    // This guardian's share of other guardians' secret keys, keyed by designated guardian id.
    internal val mySharesForOther: MutableMap<String, SecretKeyShare> = mutableMapOf()

    // Other guardians' shares of this guardian's secret key, keyed by generating guardian id.
    internal val otherSharesForMe: MutableMap<String, SecretKeyShare> = mutableMapOf()

    init {
        require(xCoordinate > 0)
        require(quorum > 0)
        // doesnt really need to contain its own PublicKeys, but leaving it for now
        guardianPublicKeys[id] = this.sendPublicKeys().unwrap()
    }

    override fun id(): String = id

    override fun xCoordinate(): Int = xCoordinate

    override fun electionPublicKey(): ElementModP = polynomial.coefficientCommitments[0]

    override fun coefficientCommitments(): List<ElementModP> = polynomial.coefficientCommitments

    override fun coefficientProofs(): List<SchnorrProof> = polynomial.coefficientProofs

    internal fun electionPrivateKey(): ElementModQ = polynomial.coefficients[0]

    override fun sendPublicKeys(): Result<PublicKeys, String> {
        return Ok(PublicKeys(
            id,
            xCoordinate,
            polynomial.coefficientProofs,
        ))
    }

    /** Receive publicKeys from another guardian.  */
    override fun receivePublicKeys(publicKeys: PublicKeys): Result<Boolean, String> {
        if (publicKeys.guardianXCoordinate < 1) {
            return Err("${publicKeys.guardianId}: guardianXCoordinate must be >= 1")
        }
        if (publicKeys.coefficientProofs.size != quorum) {
            return Err("${publicKeys.guardianId}: must have quorum ($quorum) coefficientProofs")
        }
        val validProofs: Result<Boolean, String> = publicKeys.validate()
        if (validProofs is Err) {
            return validProofs
        }

        // println("$id receivePublicKeys from ${publicKeys.guardianId}")
        guardianPublicKeys[publicKeys.guardianId] = publicKeys
        return Ok(true)
    }

    /** Create my SecretKeyShare for another guardian. */
    override fun sendSecretKeyShare(otherGuardian: String): Result<SecretKeyShare, String> {
        if (mySharesForOther.containsKey(otherGuardian)) {
            return Ok(mySharesForOther[otherGuardian]!!)
        }
        val result = generateSecretKeyShare(otherGuardian)
        if (result is Ok) {
            mySharesForOther[otherGuardian] = result.unwrap()
        }
        // println("$id sendSecretKeyShare for ${otherGuardian}")
        return result
    }

    // El(Pj(ℓ)) = spec 1.52, section 3.2.2 "share encryption"
    private fun generateSecretKeyShare(otherGuardian: String): Result<SecretKeyShare, String> {
        val other = guardianPublicKeys[otherGuardian]
            ?: return Err("Trustee '$id', does not have public key for '$otherGuardian'")

        // Compute my polynomial's y value at the other's x coordinate = Pi(ℓ)
        val Pil: ElementModQ = polynomial.valueAt(group, other.guardianXCoordinate)
        // The encryption of that, using the other's public key. See spec 1.52, section 3.2.2 eq 14.
        val EPil = Pil.byteArray().hashedElGamalEncrypt(other.publicKey())
        return Ok(
            SecretKeyShare(
                id,
                otherGuardian,
                other.guardianXCoordinate,
                EPil,
            )
        )
    }

    /** Receive and verify another guardian's SecretKeyShare for me. */
    override fun receiveSecretKeyShare(share: SecretKeyShare): Result<Boolean, String> {
         if (share.designatedGuardianId != id) {
            return Err("Sent backup to wrong trustee ${this.id}, should be trustee ${share.designatedGuardianId}")
        }

        // decrypt Pi(l)
        val secretKey = ElGamalSecretKey(this.polynomial.coefficients[0])
        val byteArray = share.encryptedCoordinate.decrypt(secretKey) // LOOK return error message
            ?: throw IllegalStateException("Trustee $id backup for ${share.generatingGuardianId} couldnt decrypt encryptedCoordinate")
        val expected: ElementModQ = byteArray.toUInt256().toElementModQ(group)

        // The Kij
        val publicKeys = guardianPublicKeys[share.generatingGuardianId]
            ?: return Err("Trustee $id does not have public keys for  ${share.generatingGuardianId}")

        // Is that value consistent with the generating guardian's commitments?
        // verify spec 1.52, sec 3.2.2 eq 16: g^Pi(ℓ) = Prod{ (Ki,j)^ℓ^j }, for j=0..k-1
        if (group.gPowP(expected) != calculateGexpPiAtL(this.xCoordinate, publicKeys.coefficientCommitments())) {
            return Err("Trustee $id failed to verify SecretKeyShare from ${share.generatingGuardianId}")
        }

        // println("$id receiveSecretKeyShare from ${share.generatingGuardianId}")
        otherSharesForMe[share.generatingGuardianId] = share
        return Ok(true)
    }

}