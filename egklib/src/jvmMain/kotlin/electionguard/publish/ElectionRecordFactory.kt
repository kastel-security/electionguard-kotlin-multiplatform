package electionguard.publish

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrapError
import electionguard.ballot.*
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.core.UInt256
import electionguard.input.ManifestInputValidation
import electionguard.util.ErrorMessages
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger("ElectionRecordFactory")

fun readElectionRecord(group : GroupContext, topDir: String) : ElectionRecord {
    val consumerIn = makeConsumer(group, topDir)
    return readElectionRecord(consumerIn)
}

// there must at least be a config record
// this throws Exceptions
fun readElectionRecord(consumer: Consumer) : ElectionRecord {
    var decryptionResult : DecryptionResult? = null
    var tallyResult : TallyResult? = null
    var init : ElectionInitialized? = null
    val config : ElectionConfig
    var stage : ElectionRecord.Stage

    val readDecryptionResult : Result<DecryptionResult, ErrorMessages> = consumer.readDecryptionResult()
    if (readDecryptionResult is Ok) {
        decryptionResult = readDecryptionResult.value
        tallyResult = decryptionResult.tallyResult
        init = tallyResult.electionInitialized
        config = init.config
        stage = ElectionRecord.Stage.DECRYPTED
    } else {
        val errs1 : ErrorMessages = readDecryptionResult.unwrapError()
        if (!errs1.contains("file does not exist")) {
            logger.error{ errs1.toString() }
            throw RuntimeException(errs1.toString())
        }
        val readTallyResult = consumer.readTallyResult()
        if (readTallyResult is Ok) {
            tallyResult = readTallyResult.value
            init = tallyResult.electionInitialized
            config = init.config
            stage = ElectionRecord.Stage.TALLIED
        } else {
            val errs2 : ErrorMessages = readTallyResult.unwrapError()
            if (!errs2.contains("file does not exist")) {
                logger.error{ errs2.toString() }
                throw RuntimeException(errs2.toString())
            }
            val readInitResult = consumer.readElectionInitialized()
            if (readInitResult is Ok) {
                init = readInitResult.value
                config = init.config
                stage = ElectionRecord.Stage.INIT
            } else {
                val errs3 : ErrorMessages = readInitResult.unwrapError()
                if (!errs3.contains("file does not exist")) {
                    logger.error{ errs3.toString() }
                    throw RuntimeException(errs3.toString())
                }
                val readConfigResult = consumer.readElectionConfig()
                if (readConfigResult is Ok) {
                    config = readConfigResult.value
                    stage = ElectionRecord.Stage.CONFIG
                } else {
                    // Always has to be a config
                    throw RuntimeException(readConfigResult.unwrapError().toString())
                }
            }
        }
    }

    require(config.manifestHash == manifestHash(config.parameterBaseHash, config.manifestBytes)) {
        "config.manifestHash fails to match ${consumer.topdir()}"
    }
    val manifest : Manifest = consumer.makeManifest(config.manifestBytes)
    val errors = ManifestInputValidation(manifest).validate()
    if (ManifestInputValidation(manifest).validate().hasErrors()) {
        throw RuntimeException("ManifestInputValidation error $errors")
    }

    if (stage == ElectionRecord.Stage.INIT && consumer.hasEncryptedBallots()) {
        stage = ElectionRecord.Stage.ENCRYPTED
    }
    return ElectionRecordImpl(consumer, stage, decryptionResult, tallyResult, init, config, manifest)
}

private class ElectionRecordImpl(val consumer: Consumer,
                                 val stageAttribute: ElectionRecord.Stage,
                                 val decryptionResultAttribute : DecryptionResult?,
                                 val tallyResultAttribute : TallyResult?,
                                 val init : ElectionInitialized?,
                                 val configAttribute : ElectionConfig,
                                 val manifestAttribute: Manifest
) : ElectionRecord {

    override fun stage(): ElectionRecord.Stage {
        return stageAttribute
    }

    override fun topdir(): String {
        return consumer.topdir()
    }

    override fun isJson(): Boolean {
        return consumer.isJson()
    }

    override fun constants(): ElectionConstants {
        return configAttribute.constants
    }

    override fun manifest(): Manifest {
        return manifestAttribute
    }

    override fun manifestBytes(): ByteArray {
        return configAttribute.manifestBytes
    }

    override fun numberOfGuardians(): Int {
        return configAttribute.numberOfGuardians
    }

    override fun quorum(): Int {
        return configAttribute.quorum
    }

    override fun config(): ElectionConfig {
        return configAttribute
    }

    override fun parameterBaseHash(): UInt256 {
        return configAttribute.parameterBaseHash
    }

    override fun electionBaseHash(): UInt256 {
        return configAttribute.electionBaseHash
    }

    override fun extendedBaseHash(): UInt256? {
        return init?.extendedBaseHash
    }

    override fun jointPublicKey(): ElementModP? {
        return init?.jointPublicKey
    }

    override fun guardians(): List<Guardian> {
        return init?.guardians ?: emptyList()
    }

    override fun electionInit(): ElectionInitialized? {
        return init
    }

    override fun encryptingDevices(): List<String> {
        return consumer.encryptingDevices()
    }

    override fun readEncryptedBallotChain(device: String): Result<EncryptedBallotChain, ErrorMessages> {
        return consumer.readEncryptedBallotChain(device)
    }

    override fun encryptedBallots(device: String, filter: ((EncryptedBallot) -> Boolean)?): Iterable<EncryptedBallot> {
        return consumer.iterateEncryptedBallots(device, filter)
    }

    override fun encryptedAllBallots(filter : ((EncryptedBallot) -> Boolean)?): Iterable<EncryptedBallot> {
        return consumer.iterateAllEncryptedBallots(filter)
    }

    override fun encryptedTally(): EncryptedTally? {
        return tallyResultAttribute?.encryptedTally
    }

    override fun tallyResult(): TallyResult? {
        return tallyResultAttribute
    }

    override fun decryptedTally(): DecryptedTallyOrBallot? {
        return decryptionResultAttribute?.decryptedTally
    }

    override fun decryptedBallots(): Iterable<DecryptedTallyOrBallot> {
        return consumer.iterateDecryptedBallots()
    }

    override fun decryptionResult(): DecryptionResult? {
        return decryptionResultAttribute
    }
}