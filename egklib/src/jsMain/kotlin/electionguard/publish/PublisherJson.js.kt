package electionguard.publish

import electionguard.model.DecryptedTallyOrBallot
import electionguard.model.DecryptionResult
import electionguard.model.ElectionConfig
import electionguard.keyceremony.KeyCeremonyTrustee
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import electionguard.util.writeFile
import electionguard.json.publishJson
import electionguard.model.ElectionInitialized
import electionguard.model.EncryptedBallot
import electionguard.model.EncryptedBallotChain
import electionguard.model.Manifest
import electionguard.model.PlaintextBallot
import electionguard.model.TallyResult
import electionguard.util.createDirectories
import electionguard.util.isDirectory
import electionguard.util.isWritable
import electionguard.util.listDir
import electionguard.util.pathExists
import node.fs.rmSync
import node.fs.rmdirSync

/** Read/write the Election Record as JSON files. */
actual class PublisherJson actual constructor(val topDir: String, createNew: Boolean) : Publisher {
    private var jsonPaths: ElectionRecordJsonPaths = ElectionRecordJsonPaths(topDir)
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }

    init {
        if (createNew) {
            removeAllFiles(topDir)
        }
        validateOutputDir(topDir)
    }

    actual override fun isJson() : Boolean = true

    actual override fun writeManifest(manifest: Manifest)  : String {
        val manifestJson = manifest.publishJson()
        writeFile(jsonPaths.manifestPath(), jsonReader.encodeToString(manifestJson))
        return jsonPaths.manifestPath()
    }

    actual override fun writeElectionConfig(config: ElectionConfig) {
        val constantsJson = config.constants.publishJson()
        val configJson = config.publishJson()
        writeFile(jsonPaths.electionConstantsPath(), jsonReader.encodeToString(constantsJson))
        writeFile(jsonPaths.manifestPath(), config.manifestBytes.decodeToString())
        writeFile(jsonPaths.electionConfigPath(), jsonReader.encodeToString(configJson))
    }

    actual override fun writeElectionInitialized(init: ElectionInitialized) {
        writeElectionConfig(init.config)

        val contextJson = init.publishJson()
        writeFile(jsonPaths.electionInitializedPath(), jsonReader.encodeToString(contextJson))
    }

    actual override fun writeTallyResult(tally: TallyResult) {
        writeElectionInitialized(tally.electionInitialized)

        val encryptedTallyJson = tally.encryptedTally.publishJson()
        writeFile(jsonPaths.encryptedTallyPath(), jsonReader.encodeToString(encryptedTallyJson))
    }

    actual override fun writeDecryptionResult(decryption: DecryptionResult) {
        writeTallyResult(decryption.tallyResult)

        val decryptedTallyJson = decryption.decryptedTally.publishJson()
        writeFile(jsonPaths.decryptedTallyPath(), jsonReader.encodeToString(decryptedTallyJson))
    }

    actual override fun writePlaintextBallot(outputDir: String, plaintextBallots: List<PlaintextBallot>) {
        plaintextBallots.forEach { writePlaintextBallot(outputDir, it) }
    }

    private fun writePlaintextBallot(outputDir: String, plaintextBallot: PlaintextBallot) {
        val plaintextBallotJson = plaintextBallot.publishJson()
        writeFile(jsonPaths.plaintextBallotPath(outputDir, plaintextBallot.ballotId),
            jsonReader.encodeToString(plaintextBallotJson))
    }

    actual override fun writeTrustee(trusteeDir: String, trustee: KeyCeremonyTrustee) {
        val decryptingTrusteeJson = trustee.publishJson()
        writeFile(jsonPaths.decryptingTrusteePath(trusteeDir, trustee.id()),
            jsonReader.encodeToString(decryptingTrusteeJson))
    }

    ////////////////////////////////////////////////

    actual override fun writeEncryptedBallotChain(closing: EncryptedBallotChain) {
        val jsonChain = closing.publishJson()
        val filename = jsonPaths.encryptedBallotChain(closing.encryptingDevice)
        writeFile(filename, jsonReader.encodeToString(jsonChain))
    }

    // batched is only used by proto, so is ignored here
    actual override fun encryptedBallotSink(device: String?, batched: Boolean): EncryptedBallotSinkIF {
        val ballotDir = if (device != null) jsonPaths.encryptedBallotDir(device) else jsonPaths.topDir
        validateOutputDir(ballotDir)
        return EncryptedBallotDeviceSink(device)
    }

    inner class EncryptedBallotDeviceSink(val device: String?) : EncryptedBallotSinkIF {

        override fun writeEncryptedBallot(ballot: EncryptedBallot) {
            val ballotFile = jsonPaths.encryptedBallotDevicePath(device, ballot.ballotId)
            val json = ballot.publishJson()
            writeFile(ballotFile, jsonReader.encodeToString(json))
        }
        override fun close() {
        }
    }

    /////////////////////////////////////////////////////////////

    actual override fun decryptedTallyOrBallotSink(): DecryptedTallyOrBallotSinkIF {
        validateOutputDir(jsonPaths.decryptedBallotDir())
        return DecryptedTallyOrBallotSink()
    }

    inner class DecryptedTallyOrBallotSink : DecryptedTallyOrBallotSinkIF {
        override fun writeDecryptedTallyOrBallot(tally: DecryptedTallyOrBallot) {
            val tallyJson = tally.publishJson()
            writeFile(jsonPaths.decryptedBallotPath(tally.id), jsonReader.encodeToString(tallyJson))
        }
        override fun close() {
        }
    }

}

/** Delete everything in the given directory, but leave that directory.  */
fun removeAllFiles(path: String) {
    if (!pathExists(path)) {
        return
    }
    listDir(path).forEach {
        if (isDirectory(it)) {
            removeAllFiles(it)
            rmdirSync(it)
        } else {
            rmSync(it)
        }
    }
}

/** Make sure output directories exists and are writeable.  */
fun validateOutputDir(path: String): Boolean {
    if (!pathExists(path)) {
        createDirectories(path)
    }
    if (!isDirectory(path)) {
        return false
    }
    if (!isWritable(path)) {
        return false
    }
    return true

}