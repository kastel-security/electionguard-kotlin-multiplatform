package electionguard.json

import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.model.EncryptedBallot
import electionguard.util.ErrorMessages
import kotlinx.serialization.Serializable

@Serializable
data class PreEncryptionJson(
    val preencryption_hash: UInt256Json,
    val all_selection_hashes: List<UInt256Json>, // size = nselections + limit, sorted numerically
    val selected_vectors: List<SelectionVectorJson>, // size = limit, sorted numerically
)

fun EncryptedBallot.PreEncryption.publishJson(): PreEncryptionJson {
    return PreEncryptionJson(
        this.preencryptionHash.publishJson(),
        this.allSelectionHashes.map { it.publishJson() },
        this.selectedVectors.map { it.publishJson() },
    )
}

fun PreEncryptionJson.import(group: GroupContext, errs: ErrorMessages): EncryptedBallot.PreEncryption? {
    val preencryptionHash = this.preencryption_hash.import() ?: errs.addNull("malformed preencryption_hash") as UInt256?
    val allSelectionHashes = this.all_selection_hashes.mapIndexed { idx, it -> it.import() ?: errs.addNull("malformed all_selection_hashes $idx") as UInt256? }
    val selectedVectors = this.selected_vectors.mapIndexed { idx,it -> it.import(group, errs.nested("selectedVectors $idx")) }

    return if (errs.hasErrors()) null
    else  EncryptedBallot.PreEncryption(
        preencryptionHash!!,
        allSelectionHashes.filterNotNull(),
        selectedVectors.filterNotNull(),
    )
}

