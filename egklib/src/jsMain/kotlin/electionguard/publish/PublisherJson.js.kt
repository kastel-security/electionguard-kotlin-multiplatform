package electionguard.publish

import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.model.*

/** Read/write the Election Record as JSON files. */
actual class PublisherJson actual constructor(topDir: String, createNew: Boolean) : Publisher {
    actual override fun isJson(): Boolean {
        TODO("Not yet implemented")
    }

    actual override fun writeManifest(manifest: Manifest): String {
        TODO("Not yet implemented")
    }

    actual override fun writeElectionConfig(config: ElectionConfig) {
    }

    actual override fun writeElectionInitialized(init: ElectionInitialized) {
    }

    actual override fun writeTallyResult(tally: TallyResult) {
    }

    actual override fun writeDecryptionResult(decryption: DecryptionResult) {
    }

    actual override fun encryptedBallotSink(
        device: String?,
        batched: Boolean
    ): EncryptedBallotSinkIF {
        TODO("Not yet implemented")
    }

    actual override fun writeEncryptedBallotChain(closing: EncryptedBallotChain) {
    }

    actual override fun decryptedTallyOrBallotSink(): DecryptedTallyOrBallotSinkIF {
        TODO("Not yet implemented")
    }

    actual override fun writePlaintextBallot(
        outputDir: String,
        plaintextBallots: List<PlaintextBallot>
    ) {
    }

    actual override fun writeTrustee(trusteeDir: String, trustee: KeyCeremonyTrustee) {
    }

}