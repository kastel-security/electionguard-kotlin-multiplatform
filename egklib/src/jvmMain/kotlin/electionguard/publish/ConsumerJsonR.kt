@file:OptIn(ExperimentalSerializationApi::class)

package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.json.*
import electionguard.json2.*
import electionguard.util.ErrorMessages
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.ByteArrayInputStream
import java.nio.file.*
import java.nio.file.spi.FileSystemProvider
import java.util.function.Predicate
import java.util.stream.Stream

private val logger = KotlinLogging.logger("ConsumerJsonRJvm")

class ConsumerJsonR(val topDir: String, val group: GroupContext) : Consumer {
    var fileSystem : FileSystem = FileSystems.getDefault()
    var fileSystemProvider : FileSystemProvider = fileSystem.provider()
    var jsonPaths = ElectionRecordJsonRPaths(topDir)
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true }

    init {
        if (!Files.exists(Path.of(topDir))) {
            throw RuntimeException("Not existent directory $topDir")
        }
        if (topDir.endsWith(".zip")) {
            val filePath = Path.of(topDir)
            fileSystem = FileSystems.newFileSystem(filePath, emptyMap<String, String>())
            val wtf = fileSystem.rootDirectories
            wtf.forEach { root ->
                Files.walk(root).forEach { path -> println(path) }
            }
            fileSystemProvider = fileSystem.provider()
            jsonPaths = ElectionRecordJsonRPaths("")
        }
    }

    override fun topdir(): String {
        return this.topDir
    }

    override fun isJson() = true

    override fun makeManifest(manifestBytes: ByteArray): Manifest {
        ByteArrayInputStream(manifestBytes).use { inp ->
            val json = jsonReader.decodeFromStream<ManifestJson>(inp)
            return json.import()
        }
    }

    override fun readManifestBytes(filename : String): ByteArray {
        // need to use fileSystemProvider for zipped files
        val manifestPath = fileSystem.getPath(filename)
        val manifestBytes =
            fileSystemProvider.newInputStream(manifestPath).use { inp ->
                inp.readAllBytes()
            }
        return manifestBytes
    }

    override fun readElectionConfig(): Result<ElectionConfig, ErrorMessages> {
        return readElectionConfig(
            fileSystem.getPath(jsonPaths.electionParametersPath()),
            fileSystem.getPath(jsonPaths.manifestCanonicalPath()),
            fileSystem.getPath(jsonPaths.electionHashesPath()),
        )
    }

    override fun readElectionInitialized(): Result<ElectionInitialized, ErrorMessages> {
        val config = readElectionConfig()
        if (config is Err) {
            return Err(config.error)
        }
        return readElectionInitialized(
            fileSystem.getPath(jsonPaths.jointElectionKeyPath()),
            config.unwrap(),
        )
    }

    private fun readElectionConfig(
        parametersFile: Path,
        manifestFile: Path,
        electionHashesFile: Path
    ): Result<ElectionConfig, ErrorMessages> {
        val errs = ErrorMessages("readElectionConfigJsonR")

        if (!Files.exists(parametersFile)) {
            errs.add("ParametersFile '$parametersFile' file does not exist ")
        }
        if (!Files.exists(manifestFile)) {
            errs.add("Manifest '$manifestFile' file does not exist ")
        }
        if (!Files.exists(electionHashesFile)) {
            errs.add("ElectionHashesFile '$electionHashesFile' file does not exist ")
        }
        if (errs.hasErrors()) {
            return Err(errs)
        }

        val parameterErrs = errs.nested("parameter file '$parametersFile'")
        var parameters : ElectionParameters? = try {
            fileSystemProvider.newInputStream(parametersFile).use { inp ->
                val json = jsonReader.decodeFromStream<ElectionParametersJsonR>(inp)
                json.import()
            }
        } catch (t: Throwable) {
            parameterErrs.add("Exception= ${t.message} ${t.stackTraceToString()}")
            null
        }

        val manifestBytes = try {
            readManifestBytes(manifestFile.toString())
        } catch (t: Throwable) {
            errs.nested("manifest file '$manifestFile'").add("Exception= ${t.message} ${t.stackTraceToString()}")
            null
        }

        val configErrs = errs.nested("ElectionHashesFile file '$electionHashesFile'")
        var electionHashes : ElectionHashes? = try {
            fileSystemProvider.newInputStream(electionHashesFile).use { inp ->
                val json = jsonReader.decodeFromStream<ElectionHashesJsonR>(inp)
                json.import(configErrs)
            }
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
            null
        }

        if (parameters == null || manifestBytes == null || electionHashes == null || errs.hasErrors()) {
            return Err(errs)
        }

        return Ok(ElectionConfig(
            "config_version TBD",
            parameters.electionConstants,
            parameters.varyingParameters.n,
            parameters.varyingParameters.k,
            electionHashes.Hp,
            electionHashes.Hm,
            electionHashes.Hb,
            manifestBytes,
            false,
            ByteArray(0),
        ))
    }

    private fun readElectionInitialized(initPath: Path, config: ElectionConfig): Result<ElectionInitialized, ErrorMessages> {
        val errs = ErrorMessages("ElectionInitializedJsonR file '${initPath}")
        if (!Files.exists(initPath)) {
            return errs.add("file does not exist ")
        }
        return try {
            fileSystemProvider.newInputStream(initPath, StandardOpenOption.READ).use { inp ->
                val json = jsonReader.decodeFromStream<ElectionInitializedJson>(inp)
                val electionInitialized = json.import(group, config, errs)
                if (errs.hasErrors()) Err(errs) else Ok(electionInitialized!!)
            }
        } catch (t: Throwable) {
            errs.add("Exception= ${t.message} ${t.stackTraceToString()}")
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    // copied from ConsumerJson, to be replaced as rust serialization is defined

    override fun encryptingDevices(): List<String> {
        val topBallotPath = Path.of(jsonPaths.encryptedBallotDir())
        if (!Files.exists(topBallotPath)) {
            return emptyList()
        }
        val deviceDirs: Stream<Path> = Files.list(topBallotPath)
        return deviceDirs.map { it.getName( it.nameCount - 1).toString() }.toList() // last name in the path
    }

    override fun readEncryptedBallotChain(device: String) : Result<EncryptedBallotChain, ErrorMessages> {
        val errs = ErrorMessages("readEncryptedBallotChain device '$device'")
        val ballotChainPath = Path.of(jsonPaths.encryptedBallotChain(device))
        if (!Files.exists(ballotChainPath)) {
            return errs.add("'$ballotChainPath' does not exist")
        }
        return try {
            fileSystemProvider.newInputStream(ballotChainPath, StandardOpenOption.READ).use { inp ->
                val json = jsonReader.decodeFromStream<EncryptedBallotChainJson>(inp)
                val chain = json.import(errs)
                if (errs.hasErrors()) Err(errs) else Ok(chain!!)
            }
        } catch (e: Exception) {
            errs.add("Exception= ${e.message}")
        }
    }

    override fun iterateEncryptedBallotsFromDir(ballotDir: String, filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot> = emptyList()


    override fun iterateEncryptedBallots(device: String, filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot> {
        val deviceDirPath = Path.of(jsonPaths.encryptedBallotDir(device))
        if (!Files.exists(deviceDirPath)) {
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

    override fun iterateAllEncryptedBallots(filter : ((EncryptedBallot) -> Boolean)? ): Iterable<EncryptedBallot> {
        val devices = encryptingDevices()
        return Iterable { DeviceIterator(devices.iterator(), filter) }
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

    //////////////////////////////////////////////////////////////////////////////////////////////////

    override fun readTallyResult(): Result<TallyResult, ErrorMessages> {
        val init = readElectionInitialized()
        if (init is Err) {
            return Err(init.error)
        }
        return readTallyResult(
            fileSystem.getPath(jsonPaths.encryptedTallyPath()),
            init.unwrap(),
        )
    }

    override fun readDecryptionResult(): Result<DecryptionResult, ErrorMessages> {
        val tally = readTallyResult()
        if (tally is Err) {
            return Err(tally.error)
        }

        return readDecryptionResult(
            fileSystem.getPath(jsonPaths.decryptedTallyPath()),
            tally.unwrap()
        )
    }

    override fun hasEncryptedBallots(): Boolean {
        return Files.exists(fileSystem.getPath(jsonPaths.encryptedBallotDir()))
    }

    // decrypted spoiled ballots
    override fun iterateDecryptedBallots(): Iterable<DecryptedTallyOrBallot> {
        val dirPath = fileSystem.getPath(jsonPaths.decryptedBallotDir())
        if (!Files.exists(dirPath)) {
            return emptyList()
        }
        return Iterable { DecryptedBallotIterator(dirPath, group) }
    }

    // plaintext ballots in given directory, with filter
    override fun iteratePlaintextBallots(
        ballotDir: String,
        filter: ((PlaintextBallot) -> Boolean)?
    ): Iterable<PlaintextBallot> {
        val dirPath = fileSystem.getPath(ballotDir)
        if (!Files.exists(dirPath)) {
            return emptyList()
        }
        return Iterable { PlaintextBallotIterator(dirPath, filter) }
    }

    // read the trustee in the given directory for the given guardianId
    override fun readTrustee(trusteeDir: String, guardianId: String): Result<DecryptingTrusteeIF,ErrorMessages> {
        val errs = ErrorMessages("readTrustee $guardianId from directory $trusteeDir")
        val filename = jsonPaths.decryptingTrusteePath(trusteeDir, guardianId)
        if (!Files.exists(fileSystem.getPath(filename))) {
            return errs.add("file does not exist ")
        }
        return readTrustee(fileSystem.getPath(filename))
    }

    override fun readEncryptedBallot(ballotDir: String, ballotId: String) : Result<EncryptedBallot, ErrorMessages> {
        val errs = ErrorMessages("readEncryptedBallot ballotId=$ballotId from directory $ballotDir")
        val ballotFilename = jsonPaths.encryptedBallotPath(ballotDir, ballotId)
        if (!Files.exists(fileSystem.getPath(ballotFilename))) {
            return errs.add("'$ballotFilename' file does not exist")
        }
        return try {
            fileSystemProvider.newInputStream(fileSystem.getPath(ballotFilename), StandardOpenOption.READ).use { inp ->
                val json = jsonReader.decodeFromStream<EncryptedBallotJson>(inp)
                val eballot = json.import(group, errs)
                if (errs.hasErrors()) Err(errs) else Ok(eballot!!)
            }
        } catch (t: Throwable) {
            return errs.add("Exception = ${t.message}")
        }
    }


    //////// The low level reading functions

    private fun readTallyResult(tallyPath: Path, init: ElectionInitialized): Result<TallyResult, ErrorMessages> {
        val errs = ErrorMessages("TallyResult file '${tallyPath}'")
        if (!Files.exists(tallyPath)) {
            return errs.add("does not exist")
        }
        return try {
            fileSystemProvider.newInputStream(tallyPath, StandardOpenOption.READ).use { inp ->
                val json = jsonReader.decodeFromStream<EncryptedTallyJson>(inp)
                val encryptedTally = json.import(group, errs)
                if (errs.hasErrors()) Err(errs) else Ok(TallyResult(init, encryptedTally!!, emptyList()))
            }
        } catch (e: Exception) {
            errs.add("Exception= ${e.message}")
        }
    }

    private fun readDecryptionResult(
        decryptedTallyPath: Path,
        tallyResult: TallyResult
    ): Result<DecryptionResult, ErrorMessages> {
        val errs = ErrorMessages("DecryptedTally '$decryptedTallyPath'")
        if (!Files.exists(decryptedTallyPath)) {
            return errs.add("file does not exist ")
        }
        return try {
            fileSystemProvider.newInputStream(decryptedTallyPath, StandardOpenOption.READ).use { inp ->
                val json = jsonReader.decodeFromStream<DecryptedTallyOrBallotJson>(inp)
                val decryptedTallyOrBallot = json.import(group, errs)
                if (errs.hasErrors()) Err(errs) else Ok(DecryptionResult(tallyResult, decryptedTallyOrBallot!!))
            }
        } catch (e: Exception) {
            errs.add("Exception= ${e.message}")
        }
    }

    private fun readTrustee(filePath: Path): Result<DecryptingTrusteeIF, ErrorMessages> {
        val errs = ErrorMessages("readTrustee '$filePath'")
        return try {
            fileSystemProvider.newInputStream(filePath, StandardOpenOption.READ).use { inp ->
                val json = jsonReader.decodeFromStream<TrusteeJson>(inp)
                val decryptingTrustee = json.importDecryptingTrustee(group, errs)
                if (errs.hasErrors()) Err(errs) else Ok(decryptingTrustee!!)
            }
        } catch (e: Exception) {
            errs.add("Exception= ${e.message}")
        }
    }

    private inner class PlaintextBallotIterator(
        ballotDir: Path,
        private val filter: Predicate<PlaintextBallot>?
    ) : AbstractIterator<PlaintextBallot>() {
        val pathList = ballotDir.pathListNoDirs()
        var idx = 0

        override fun computeNext() {
            while (idx < pathList.size) {
                val file = pathList[idx++]
                fileSystemProvider.newInputStream(file, StandardOpenOption.READ).use { inp ->
                    val json = jsonReader.decodeFromStream<PlaintextBallotJson>(inp)
                    val plaintextBallot = json.import()
                    if (filter == null || filter.test(plaintextBallot)) {
                        setNext(plaintextBallot)
                        return
                    }
                }
            }
            return done()
        }
    }

    //// Encrypted ballots iteration

    private inner class EncryptedBallotFileIterator(
        ballotDir: Path,
        private val filter: Predicate<EncryptedBallot>?,
    ) : AbstractIterator<EncryptedBallot>() {
        val pathList = ballotDir.pathListNoDirs()
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
                        if (filter == null || filter.test(encryptedBallot!!)) {
                            return setNext(encryptedBallot!!)
                        } // otherwise skip it
                    }
                } catch (t : Throwable) {
                    println("Error reading EncryptedBallot '${ballotFilePath}', skipping.\n  ${t.message}")
                    logger.error { "Error reading EncryptedBallot '${ballotFilePath}', skipping.\n  ${t.message}"}
                }
            }
            return done()
        }
    }

    private inner class EncryptedBallotDeviceIterator(
        val device: String,
        val ballotIds: Iterator<String>,
        private val filter: Predicate<EncryptedBallot>?,
    ) : AbstractIterator<EncryptedBallot>() {

        override fun computeNext() {
            while (true) {
                if (ballotIds.hasNext()) {
                    val ballotFilePath : Path = Path.of(jsonPaths.encryptedBallotDevicePath(device, ballotIds.next()))
                    try {
                        val errs = ErrorMessages("EncryptedBallotJson '$ballotFilePath'")
                        val encryptedBallot = readEncryptedBallot(ballotFilePath, errs)
                        if (errs.hasErrors()) {
                            logger.error { errs.toString() }
                        } else {
                            if (filter == null || filter.test(encryptedBallot!!)) {
                                return setNext(encryptedBallot!!)
                            } // otherwise skip it
                        }
                    } catch (t : Throwable) {
                        println("Error reading EncryptedBallot '${ballotFilePath}', skipping.\n  ${t.message}")
                        logger.error { "Error reading EncryptedBallot '${ballotFilePath}', skipping.\n  ${t.message}"}
                    }
                } else {
                    return done()
                }
            }
        }
    }

    fun readEncryptedBallot(ballotFilePath : Path, errs: ErrorMessages): EncryptedBallot? {
        fileSystemProvider.newInputStream(ballotFilePath, StandardOpenOption.READ).use { inp ->
            val json = jsonReader.decodeFromStream<EncryptedBallotJson>(inp)
            return json.import(group, errs)
        }
    }

    //// Decrypted ballots iteration

    private inner class DecryptedBallotIterator(
        ballotDir: Path,
        private val group: GroupContext,
    ) : AbstractIterator<DecryptedTallyOrBallot>() {
        val pathList = ballotDir.pathListNoDirs()
        var idx = 0

        override fun computeNext() {
            while (idx < pathList.size) {
                val file = pathList[idx++]
                fileSystemProvider.newInputStream(file, StandardOpenOption.READ).use { inp ->
                    val json = jsonReader.decodeFromStream<DecryptedTallyOrBallotJson>(inp)
                    val errs = ErrorMessages("DecryptedBallotIterator '$file'")
                    val decryptedTallyOrBallot = json.import(group, errs)
                    if (errs.hasErrors()) {
                        logger.error { errs.toString() }
                    } else {
                        return setNext(decryptedTallyOrBallot!!)
                    }
                }
            }
            return done()
        }
    }

}