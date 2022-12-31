package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.toResultOr
import com.github.michaelbull.result.unwrap
import electionguard.core.ElGamalKeypair
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElGamalSecretKey
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeDoerre
import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.keyceremony.EncryptedKeyShare

fun electionguard.protogen.DecryptingTrustee.import(group: GroupContext):
        Result<DecryptingTrusteeDoerre, String> {

    val id = this.guardianId
    val publicKey = group.importElementModP(this.publicKey) .toResultOr { "DecryptingTrustee $id publicKey was malformed or missing" }
    val keyShare = group.importElementModQ(this.keyShare) .toResultOr { "DecryptingTrustee $id keyShare was malformed or missing" }

    val errors = getAllErrors(publicKey, keyShare)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(DecryptingTrusteeDoerre(
        this.guardianId,
        this.guardianXCoordinate,
        publicKey.unwrap(),
        keyShare.unwrap(),
    ))
}

private fun electionguard.protogen.ElGamalKeypair.import(id: String, group: GroupContext):
        Result<ElGamalKeypair, String> {

    val secretKey = group.importElementModQ(this.secretKey)
        .toResultOr { "DecryptingTrustee $id secretKey was malformed or missing" }
    val publicKey = group.importElementModP(this.publicKey)
        .toResultOr { "DecryptingTrustee $id publicKey was malformed or missing" }

    val errors = getAllErrors(secretKey, publicKey)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }
    return Ok(
        ElGamalKeypair(
            ElGamalSecretKey(secretKey.unwrap()),
            ElGamalPublicKey(publicKey.unwrap()),
        )
    )
}

private fun electionguard.protogen.EncryptedKeyShare.import(id: String, group: GroupContext):
        Result<EncryptedKeyShare, String> {

    val encryptedCoordinate = group.importHashedCiphertext(this.encryptedCoordinate)
        .toResultOr { "DecryptingTrustee $id secretKey was malformed or missing" }

    if (encryptedCoordinate is Err) {
        return encryptedCoordinate
    }
    return Ok(
        EncryptedKeyShare(
            this.polynomialOwner,
            this.secretShareFor,
            encryptedCoordinate.unwrap(),
        )
    )
}

///////////////////////////////////////////////////////////////////////////////

fun KeyCeremonyTrustee.publishDecryptingTrusteeProto() =
    electionguard.protogen.DecryptingTrustee(
        this.id(),
        this.xCoordinate(),
        this.electionPublicKey().publishProto(),
        this.keyShare().publishProto(),
    )

private fun ElGamalKeypair.publishProto() =
    electionguard.protogen.ElGamalKeypair(
        this.secretKey.key.publishProto(),
        this.publicKey.key.publishProto(),
    )

private fun EncryptedKeyShare.publishProto() =
    electionguard.protogen.EncryptedKeyShare(
        this.polynomialOwner,
        this.secretShareFor,
        this.encryptedCoordinate.publishProto(),
    )
