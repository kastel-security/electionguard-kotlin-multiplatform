package electionguard.publish

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.DecryptionResult
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionConstants
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.EncryptedBallotChain
import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextBallot
import electionguard.ballot.TallyResult
import electionguard.core.GroupContext
import electionguard.core.fileReadBytes
import electionguard.core.fileReadText
import electionguard.core.isDirectory
import electionguard.core.pathExists
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.json2.DecryptedTallyOrBallotJson
import electionguard.json2.ElectionConfigJson
import electionguard.json2.ElectionConstantsJson
import electionguard.json2.ElectionInitializedJson
import electionguard.json2.EncryptedBallotChainJson
import electionguard.json2.EncryptedBallotJson
import electionguard.json2.EncryptedTallyJson
import electionguard.json2.PlaintextBallotJson
import electionguard.json2.TrusteeJson
import electionguard.json2.import
import electionguard.json2.importDecryptingTrustee
import electionguard.util.ErrorMessages
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import node.buffer.BufferEncoding
import node.fs.readdirSync
import node.path.path

private val logger = KotlinLogging.logger("ConsumerJsonJs")

actual class ConsumerJson actual constructor(
    val topDir: String,
    val group: GroupContext) : Consumer {
    var jsonPaths = ElectionRecordJsonPaths(topDir)
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true; prettyPrint = true }

    init {
        if (!pathExists(topDir)) {
            throw RuntimeException("Not existent directory $topDir")
        }
        //TODO handle zip somehow?
    }

    actual override fun topdir(): String = topDir

    actual override fun isJson(): Boolean = true

    actual override fun readManifestBytes(filename: String): ByteArray {
        return fileReadBytes(filename)
    }

    actual override fun makeManifest(manifestBytes: ByteArray): Manifest {
        return jsonReader.decodeFromString(manifestBytes.decodeToString())
    }

    actual override fun readElectionConfig(): Result<ElectionConfig, ErrorMessages> {
        val constantsPath = jsonPaths.electionConstantsPath()
        val manifestPath = jsonPaths.manifestPath()
        val configPath = jsonPaths.electionConfigPath()
        val errs = ErrorMessages("readElectionConfigJson")

        if (!pathExists(constantsPath)) {
            errs.add("Constants '$constantsPath' file does not exist ")
        }
        if (!pathExists(manifestPath)) {
            errs.add("Manifest '$manifestPath' file does not exist ")
        }
        if (!pathExists(configPath)) {
            errs.add("ElectionConfig '$configPath' file does not exist ")
        }
        if (errs.hasErrors()) {
            return Err(errs)
        }

        val constantErrs = errs.nested("constants file '$constantsPath'")
        val constants: ElectionConstants? = try {
            val constantsJson = jsonReader.decodeFromString<ElectionConstantsJson>(fileReadText(constantsPath))
            constantsJson.import(constantErrs)
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
            null
        }

        val manifestBytes = try {
            readManifestBytes(manifestPath)
        } catch (t: Throwable) {
            errs.nested("manifest file '$manifestPath'").add("Exception= ${t.message} ${t.stackTraceToString()}")
            null
        }

        val configErrs = errs.nested("config file '$configPath'")
        return try {
            val configJson = jsonReader.decodeFromString<ElectionConfigJson>(fileReadText(configPath))
            val electionConfig = configJson.import(constants, manifestBytes, errs)
            if (errs.hasErrors()) Err(errs) else Ok(electionConfig!!)
        } catch (t: Throwable) {
            return configErrs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }

    }

    actual override fun readElectionInitialized(): Result<ElectionInitialized, ErrorMessages> {
        val initPath = jsonPaths.electionInitializedPath()
        val configResult = readElectionConfig()
        if (configResult is Err) {
            return Err(configResult.error)
        }
        val config = configResult.unwrap()

        val errs = ErrorMessages("ElectionInitializedJson file '${initPath}")
        if (!pathExists(initPath)) {
            return errs.add("file does not exist ")
        }
        return try {
            val initJson = jsonReader.decodeFromString<ElectionInitializedJson>(fileReadText(initPath))
            val electionInitialized = initJson.import(group, config, errs)
            if (errs.hasErrors()) Err(errs) else Ok(electionInitialized!!)
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
    }

    actual override fun readTallyResult(): Result<TallyResult, ErrorMessages> {
        val tallyPath = jsonPaths.encryptedTallyPath()
        val initResult = readElectionInitialized()
        if (initResult is Err) {
            return Err(initResult.error)
        }
        val init = initResult.unwrap()

        val errs = ErrorMessages("TallyResult file '${tallyPath}'")
        if (!pathExists(tallyPath)) {
            return errs.add("file does not exist")
        }
        return try {
            val json = jsonReader.decodeFromString<EncryptedTallyJson>(fileReadText(tallyPath))
            val encryptedTally = json.import(group, errs)
            if (errs.hasErrors()) Err(errs) else Ok(TallyResult(init, encryptedTally!!, emptyList()))
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
    }

    actual override fun readDecryptionResult(): Result<DecryptionResult, ErrorMessages> {
        val decryptedTallyPath = jsonPaths.decryptedTallyPath()
        val tally = readTallyResult()
        if (tally is Err) {
            return Err(tally.error)
        }
        val tallyResult = tally.unwrap()

        val errs = ErrorMessages("DecryptedTally '$decryptedTallyPath'")
        if (!pathExists(decryptedTallyPath)) {
            return errs.add("file does not exist ")
        }
        return try {
            val json = jsonReader.decodeFromString<DecryptedTallyOrBallotJson>(fileReadText(decryptedTallyPath))
            val decryptedTallyOrBallot = json.import(group, errs)
            if (errs.hasErrors()) Err(errs) else Ok(DecryptionResult(tallyResult, decryptedTallyOrBallot!!))
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
    }

    actual override fun hasEncryptedBallots(): Boolean {
        val iter = iterateAllEncryptedBallots { true }
        return iter.iterator().hasNext()
    }

    actual override fun encryptingDevices(): List<String> {
        val topBallotPath = jsonPaths.encryptedBallotDir()
        if (!pathExists(topBallotPath) || !isDirectory(topBallotPath)) {
            return emptyList()
        }
        //This is required to resolve overload ambiguity
        val deviceDirs = listDir(topBallotPath)
        return deviceDirs.map { path.parse(it).name }
    }

    actual override fun readEncryptedBallotChain(device: String): Result<EncryptedBallotChain, ErrorMessages> {
        val errs = ErrorMessages("readEncryptedBallotChain device '$device'")
        val ballotChainPath = jsonPaths.encryptedBallotChain(device)
        if (!pathExists(ballotChainPath)) {
            return errs.add("'$ballotChainPath' file does not exist")
        }
        return try {
            val json = jsonReader.decodeFromString<EncryptedBallotChainJson>(fileReadText(ballotChainPath))
            val chain = json.import(errs)
            if (errs.hasErrors()) Err(errs) else Ok(chain!!)
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
    }

    actual override fun readEncryptedBallot(
        ballotDir: String,
        ballotId: String
    ): Result<EncryptedBallot, ErrorMessages> {
        val errs = ErrorMessages("readEncryptedBallot ballotId=$ballotId from directory $ballotDir")
        val ballotFilename = jsonPaths.encryptedBallotPath(ballotDir, ballotId)
        if (!pathExists(ballotFilename)) {
            return errs.add("'$ballotFilename' file does not exist")
        }
        return try {
            val eballot = readEncryptedBallot(ballotFilename, errs)
            if (errs.hasErrors()) Err(errs) else Ok(eballot!!)
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
    }

    actual override fun iterateEncryptedBallots(
        device: String,
        filter: ((EncryptedBallot) -> Boolean)?
    ): Iterable<EncryptedBallot> {
        val deviceDirPath = jsonPaths.encryptedBallotDir(device)
        if (!pathExists(deviceDirPath)) {
            throw RuntimeException("ConsumerJson.iterateEncryptedBallots: $deviceDirPath doesnt exist")
        }
        val chainResult = readEncryptedBallotChain(device)
        if (chainResult is Ok) {
            val chain = chainResult.unwrap()
            return Iterable { EncryptedBallotDeviceIterator(device, chain.ballotIds.iterator(), filter) }
        }
        // just read individual files
        return Iterable { EncryptedBallotFileIterator(deviceDirPath, filter) }
    }

    actual override fun iterateAllEncryptedBallots(filter: ((EncryptedBallot) -> Boolean)?): Iterable<EncryptedBallot> {
        val devices = encryptingDevices()
        return Iterable { DeviceIterator(devices.iterator(), filter) }
    }

    actual override fun iterateDecryptedBallots(): Iterable<DecryptedTallyOrBallot> {
        val dirPath = jsonPaths.decryptedBallotDir()
        if (!pathExists(dirPath)) {
            return emptyList()
        }
        return Iterable { DecryptedBallotIterator(dirPath, group) }
    }

    actual override fun iterateEncryptedBallotsFromDir(
        ballotDir: String,
        filter: ((EncryptedBallot) -> Boolean)?
    ): Iterable<EncryptedBallot> {
        if (!pathExists(ballotDir)) {
            return emptyList()
        }
        return Iterable { EncryptedBallotFileIterator(ballotDir, filter) }
    }

    actual override fun iteratePlaintextBallots(
        ballotDir: String,
        filter: ((PlaintextBallot) -> Boolean)?
    ): Iterable<PlaintextBallot> {
        if (!pathExists(ballotDir)) {
            return emptyList()
        }
        return Iterable { PlaintextBallotIterator(ballotDir, filter) }
    }

    actual override fun readTrustee(
        trusteeDir: String,
        guardianId: String
    ): Result<DecryptingTrusteeIF, ErrorMessages> {
        val fileErrs = ErrorMessages("readTrustee $guardianId from directory $trusteeDir")
        val filename = jsonPaths.decryptingTrusteePath(trusteeDir, guardianId)
        if (!pathExists(filename)) {
            return fileErrs.add("file does not exist ")
        }

        val errs = ErrorMessages("readTrustee '$filename'")
        return try {
            val json = jsonReader.decodeFromString<TrusteeJson>(fileReadText(filename))
            val decryptingTrustee = json.importDecryptingTrustee(group, errs)
            if (errs.hasErrors()) Err(errs) else Ok(decryptingTrustee!!)
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
    }

    private fun readEncryptedBallot(ballotFilePath: String, errs: ErrorMessages): EncryptedBallot? {
        val json = jsonReader.decodeFromString<EncryptedBallotJson>(fileReadText(ballotFilePath))
        return json.import(group, errs)
    }

    //// Encrypted ballots iteration

    private inner class EncryptedBallotFileIterator(
        ballotDir: String,
        private val filter: ((EncryptedBallot) -> Boolean)?,
    ) : AbstractIterator<EncryptedBallot>() {
        val pathList = getFilesNoDir(ballotDir)
        var idx = 0

        override fun computeNext() {
            while (idx < pathList.size) {
                val ballotFilePath = pathList[idx++]
                try {
                    val errs = ErrorMessages("EncryptedBallotJson '$ballotFilePath'")
                    val encryptedBallot = readEncryptedBallot(ballotFilePath, errs)
                    if (errs.hasErrors()) {
                        logger.error { errs.toString() }
                    } else {
                        if (filter == null || filter.invoke(encryptedBallot!!)) {
                            return setNext(encryptedBallot!!)
                        } // otherwise skip it
                    }
                } catch (t : Throwable) {
                    println("Error reading EncryptedBallot '${ballotFilePath}', skipping.\n  Exception= ${t.message} ${t.stackTraceToString()}")
                    logger.error { "Error reading EncryptedBallot '${ballotFilePath}', skipping.\n Exception= ${t.message} ${t.stackTraceToString()}"}
                }
            }
            return done()
        }
    }

    private inner class EncryptedBallotDeviceIterator(
        val device: String,
        val ballotIds: Iterator<String>,
        val filter: ((EncryptedBallot) -> Boolean)?
    ) : AbstractIterator<EncryptedBallot>() {

        override fun computeNext() {
            while (true) {
                if (ballotIds.hasNext()) {
                    val ballotFilePath = jsonPaths.encryptedBallotDevicePath(device, ballotIds.next())
                    if (!pathExists(ballotFilePath)) {
                        println("EncryptedBallotDeviceIterator file '${ballotFilePath}' does not exist, skipping}")
                        continue
                    }
                    try {
                        val errs = ErrorMessages("EncryptedBallotJson '$ballotFilePath'")
                        val encryptedBallot = readEncryptedBallot(ballotFilePath, errs)
                        if (errs.hasErrors()) {
                            logger.error { errs.toString() }
                        } else {
                            if (filter == null || filter.invoke(encryptedBallot!!)) {
                                return setNext(encryptedBallot!!)
                            }
                        }
                    } catch (t : Throwable) {
                        println("Error reading EncryptedBallot '${ballotFilePath}', skipping.\n  Exception= ${t.message} ${t.stackTraceToString()}")
                        logger.error { "Error reading EncryptedBallot '${ballotFilePath}', skipping.\n  Exception= ${t.message} ${t.stackTraceToString()}"}
                    }
                } else {
                    return done()
                }
            }
        }
    }

    private inner class DeviceIterator(
        val devices: Iterator<String>,
        private val filter : ((EncryptedBallot) -> Boolean)?,
    ) : AbstractIterator<EncryptedBallot>() {
        var innerIterator: Iterator<EncryptedBallot>? = null

        override fun computeNext() {
            while (true) {
                if (innerIterator != null && innerIterator!!.hasNext()) {
                    return setNext(innerIterator!!.next())
                }
                if (devices.hasNext()) {
                    innerIterator = iterateEncryptedBallots(devices.next(), filter).iterator()
                } else {
                    return done()
                }
            }
        }
    }

    private inner class PlaintextBallotIterator(
        ballotDir: String,
        private val filter: ((PlaintextBallot) -> Boolean)?
    ) : AbstractIterator<PlaintextBallot>() {
        val pathList = getFilesNoDir(ballotDir)
        var idx = 0

        override fun computeNext() {
            while (idx < pathList.size) {
                val file = pathList[idx++]
                val json = jsonReader.decodeFromString<PlaintextBallotJson>(fileReadText(file))
                val plaintextBallot = json.import()
                if (filter == null || filter.invoke(plaintextBallot)) {
                    setNext(plaintextBallot)
                    return
                }
            }
            return done()
        }
    }

    //// Decrypted ballots iteration

    private inner class DecryptedBallotIterator(
        ballotDir: String,
        private val group: GroupContext,
    ) : AbstractIterator<DecryptedTallyOrBallot>() {
        val pathList = getFilesNoDir(ballotDir)
        var idx = 0

        override fun computeNext() {
            while (idx < pathList.size) {
                val file = pathList[idx++]
                val json = jsonReader.decodeFromString<DecryptedTallyOrBallotJson>(fileReadText(file))
                val errs = ErrorMessages("DecryptedBallotIterator '$file'")
                val decryptedTallyOrBallot = json.import(group, errs)
                if (errs.hasErrors()) {
                    logger.error { errs.toString() }
                } else {
                    return setNext(decryptedTallyOrBallot!!)
                }
            }
            return done()
        }
    }

    fun getFilesNoDir(path: String): List<String> {
        return listDir(path).filter { !isDirectory(it) }
    }

    fun listDir(path: String): List<String> {
        return readdirSync(path, options = null as BufferEncoding?).toList().map { node.path.path.join(path, it) }
    }
}