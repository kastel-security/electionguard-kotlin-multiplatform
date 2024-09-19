package electionguard.publish

import com.github.michaelbull.result.Result
import electionguard.model.*
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.util.ErrorMessages

actual class ConsumerJsonR actual constructor(topDir: String, group: GroupContext) :
    Consumer {
    actual override fun topdir(): String {
        TODO("Not yet implemented")
    }

    actual override fun isJson(): Boolean {
        TODO("Not yet implemented")
    }

    actual override fun readManifestBytes(filename: String): ByteArray {
        TODO("Not yet implemented")
    }

    actual override fun makeManifest(manifestBytes: ByteArray): Manifest {
        TODO("Not yet implemented")
    }

    actual override fun readElectionConfig(): Result<ElectionConfig, ErrorMessages> {
        TODO("Not yet implemented")
    }

    actual override fun readElectionInitialized(): Result<ElectionInitialized, ErrorMessages> {
        TODO("Not yet implemented")
    }

    actual override fun readTallyResult(): Result<TallyResult, ErrorMessages> {
        TODO("Not yet implemented")
    }

    actual override fun readDecryptionResult(): Result<DecryptionResult, ErrorMessages> {
        TODO("Not yet implemented")
    }

    actual override fun hasEncryptedBallots(): Boolean {
        TODO("Not yet implemented")
    }

    actual override fun encryptingDevices(): List<String> {
        TODO("Not yet implemented")
    }

    actual override fun readEncryptedBallotChain(device: String): Result<EncryptedBallotChain, ErrorMessages> {
        TODO("Not yet implemented")
    }

    actual override fun readEncryptedBallot(
        ballotDir: String,
        ballotId: String
    ): Result<EncryptedBallot, ErrorMessages> {
        TODO("Not yet implemented")
    }

    actual override fun iterateEncryptedBallots(
        device: String,
        filter: ((EncryptedBallot) -> Boolean)?
    ): Iterable<EncryptedBallot> {
        TODO("Not yet implemented")
    }

    actual override fun iterateAllEncryptedBallots(filter: ((EncryptedBallot) -> Boolean)?): Iterable<EncryptedBallot> {
        TODO("Not yet implemented")
    }

    actual override fun iterateDecryptedBallots(): Iterable<DecryptedTallyOrBallot> {
        TODO("Not yet implemented")
    }

    actual override fun iterateEncryptedBallotsFromDir(
        ballotDir: String,
        filter: ((EncryptedBallot) -> Boolean)?
    ): Iterable<EncryptedBallot> {
        TODO("Not yet implemented")
    }

    actual override fun iteratePlaintextBallots(
        ballotDir: String,
        filter: ((PlaintextBallot) -> Boolean)?
    ): Iterable<PlaintextBallot> {
        TODO("Not yet implemented")
    }

    actual override fun readTrustee(
        trusteeDir: String,
        guardianId: String
    ): Result<DecryptingTrusteeIF, ErrorMessages> {
        TODO("Not yet implemented")
    }

}