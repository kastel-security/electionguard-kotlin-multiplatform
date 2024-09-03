package electionguard.verifier

import com.github.michaelbull.result.*
import electionguard.ballot.*
import electionguard.core.*
import electionguard.publish.ElectionRecord
import electionguard.util.ErrorMessages
import electionguard.util.Stats

class Verifier(val record: ElectionRecord, val nthreads: Int = 11) {
    val group: GroupContext = productionGroup()
    val manifest: ManifestIF
    val jointPublicKey: ElGamalPublicKey
    val He: UInt256

    init {
        manifest = record.manifest()

        if (record.stage() < ElectionRecord.Stage.INIT) { // fake
            He = UInt256.random()
            jointPublicKey = ElGamalPublicKey(group.ONE_MOD_P)
        } else {
            jointPublicKey = ElGamalPublicKey(record.jointPublicKey()!!)
            He = record.extendedBaseHash()!!
        }
    }

    fun verify(stats : Stats, showTime : Boolean = false): Boolean {
        println("\n****Verify election record in = ${record.topdir()}\n")
        val starting13 = getSystemTimeInMillis()
        val config = record.config()

        val parametersOk = verifyParameters(config, record.manifestBytes())
        println(" 1. verifyParameters= $parametersOk")

        if (record.stage() < ElectionRecord.Stage.INIT) {
            println("election record stage = ${record.stage()}, stopping verification now\n")
            val took = getSystemTimeInMillis() - starting13
            if (showTime) println("   verify `took $took millisecs")
            return true
        }

        val guardiansOk = verifyGuardianPublicKey()
        println(" 2. verifyGuardianPublicKeys= $guardiansOk")

        val publicKeyOk = verifyElectionPublicKey()
        println(" 3. verifyElectionPublicKey= $publicKeyOk")

        val baseHashOk = verifyExtendedBaseHash()
        println(" 4. verifyExtendedBaseHash= $baseHashOk")

        if (record.stage() < ElectionRecord.Stage.ENCRYPTED) {
            println("election record stage = ${record.stage()}, stopping verification now\n")
            val took = getSystemTimeInMillis() - starting13
            if (showTime) println("   verify 2,3,4 took $took millisecs")
            return true
        }

        // encryption and vote limits
        val encryptionVerifier = VerifyEncryptedBallots(group, manifest, jointPublicKey, He, config, nthreads)
        // Note we are validating all ballots, not just CAST, and including preencrypted
        val eerrs = ErrorMessages("")
        val ballotsOk = encryptionVerifier.verifyBallots(record.encryptedAllBallots { true }, eerrs, stats, showTime)
        println(" 5,6,15,16,17,18. verifyEncryptedBallots $ballotsOk")
        if (!ballotsOk) {
            println(eerrs)
        }

        val chainOk = if (!config.chainConfirmationCodes) true else {
            val chainErrs = ErrorMessages("")
            val ok = encryptionVerifier.verifyConfirmationChain(record, chainErrs)
            println(" 7. verifyConfirmationChain $ok")
            if (!ok) {
                println(chainErrs)
            }
            ok
        }

        if (record.stage() < ElectionRecord.Stage.TALLIED) {
            println("election record stage = ${record.stage()}, stopping verification now\n")
            return true
        }

        // tally accumulation
        val tallyVerifier = VerifyTally(group, encryptionVerifier.aggregator)
        val tallyErrs = ErrorMessages("")
        val tallyOk = tallyVerifier.verify(record.encryptedTally()!!, tallyErrs, showTime)
        println(" 8. verifyBallotAggregation $tallyOk")
        if (!tallyOk) {
            println(tallyErrs)
        }

        if (record.stage() < ElectionRecord.Stage.DECRYPTED) {
            println("election record stage = ${record.stage()}, stopping verification now\n")
            return true
        }

        // tally decryption
        val decryptionVerifier = VerifyDecryption(group, manifest, jointPublicKey, He)
        val tdErrs = ErrorMessages("")
        val tdOk = decryptionVerifier.verify(record.decryptedTally()!!, isBallot = false, tdErrs, stats)
        println(" 9,10,11. verifyTallyDecryption $tdOk")
        if (!tdOk) {
            println(tdErrs)
        }

        // 12, 13, 14 spoiled ballots
        val spoiledErrs = ErrorMessages("")
        val spoiledOk =
            decryptionVerifier.verifySpoiledBallotTallies(record.decryptedBallots(), nthreads, spoiledErrs, stats, showTime)
        println(" 12,13,14. verifySpoiledBallotTallies $spoiledOk")
        if (!spoiledOk) {
            println(spoiledErrs)
        }

        val allOk = (parametersOk is Ok) && (guardiansOk is Ok) && (publicKeyOk is Ok) && (baseHashOk is Ok) &&
                ballotsOk && chainOk && tallyOk && tdOk && spoiledOk
        println("verify allOK = $allOk\n")
        return allOk
    }

    // Verification 1 (Parameter validation)
    private fun verifyParameters(config : ElectionConfig, manifestBytes: ByteArray): Result<Boolean, String> {
        val check: MutableList<Result<Boolean, String>> = mutableListOf()
        val constants = config.constants

        if (config.configVersion != protocolVersion) {
            check.add(Err("  1.A The election record specification version '${config.configVersion}' does not match '$protocolVersion'"))
        }
        if (!constants.largePrime.contentEquals(group.constants.largePrime)) {
            check.add(Err("  1.B The large prime is not equal to p defined in Section 3.1.1"))
        }
        if (!constants.smallPrime.contentEquals(group.constants.smallPrime)) {
            check.add(Err("  1.C The small prime is not equal to q defined in Section 3.1.1"))
        }
        if (!constants.cofactor.contentEquals(group.constants.cofactor)) {
            check.add(Err("  1.D The cofactor is not equal to r defined in Section 3.1.1"))
        }
        if (!constants.generator.contentEquals(group.constants.generator)) {
            check.add(Err("  1.E The generator is not equal to g defined in Section 3.1.1"))
        }

        val Hp = parameterBaseHash(config.constants)
        if (Hp != config.parameterBaseHash) {
            check.add(Err("  1.F The parameter base hash does not match eq 4"))
        }
        val Hm = manifestHash(Hp, manifestBytes)
        if (Hm != config.manifestHash) {
            check.add(Err("  1.G The manifest hash does not match eq 5"))
        }
        val Hd = electionBaseHash(Hp, Hm, config.numberOfGuardians, config.quorum)
        if (Hd != config.electionBaseHash) {
            check.add(Err("  1.H The election base hash does not match eq 6"))
        }

        return check.merge()
    }

    // Verification Box 2
    private fun verifyGuardianPublicKey(): Result<Boolean, String> {
        val checkProofs: MutableList<Result<Boolean, String>> = mutableListOf()
        for (guardian in this.record.guardians()) {
            guardian.coefficientProofs.forEachIndexed { index, proof ->
                val result = proof.validate(guardian.xCoordinate, index)
                if (result is Err) {
                    checkProofs.add(Err("  2. Guardian ${guardian.guardianId} has invalid proof for coefficient $index " +
                        result.unwrapError()
                    ))
                }
            }
        }
        return checkProofs.merge()
    }

    // Verification 3 (Election public-key validation)
    //An election verifier must verify the correct computation of the joint election public key.
    //(3.A) The value Ki is in Zpr and Ki  ̸= 1 mod p
    private fun verifyElectionPublicKey(): Result<Boolean, String> {
        val errors = mutableListOf<Result<Boolean, String>>()

        val guardiansSorted = this.record.guardians().sortedBy { it.xCoordinate }
        guardiansSorted.forEach {
            val Ki = it.publicKey()
            if (!Ki.isValidResidue()) {
                errors.add(Err("  3.A publicKey Ki (${it.guardianId} is not in Zp^r"))
            }
            if (Ki == group.ONE_MOD_P) {
                errors.add(Err("  3.A publicKey Ki is equal to ONE_MOD_P"))
            }
        }

        val jointPublicKeyComputed = guardiansSorted.map { it.publicKey() }.reduce { a, b -> a * b }
        if (!jointPublicKey.equals(jointPublicKeyComputed)) {
            errors.add(Err("  3.B jointPublicKey K does not equal computed K = Prod(K_i)"))
        }
        return errors.merge()
    }

    private fun verifyExtendedBaseHash(): Result<Boolean, String> {
        val errors = mutableListOf<Result<Boolean, String>>()
        val guardiansSorted = this.record.guardians().sortedBy { it.xCoordinate }

        val commitments = mutableListOf<ElementModP>()
        guardiansSorted.forEach { commitments.addAll(it.coefficientCommitments()) }
        require(record.quorum() * record.numberOfGuardians() == commitments.size)

        // He = H(HB ; 0x12, K) ; spec 2.0.0 p.25, eq 23.
        val computeHe = electionExtendedHash(record.electionBaseHash(), jointPublicKey.key)
        if (He != computeHe) {
            errors.add(Err("  4.A extendedBaseHash  does not match computed"))
        }
        return errors.merge()
    }

    fun verifyEncryptedBallots(stats : Stats): ErrorMessages {
        val errs = ErrorMessages("verifyDecryptedTally")
        val verifyBallots = VerifyEncryptedBallots(group, manifest, jointPublicKey, He, record.config(), nthreads)
        verifyBallots.verifyBallots(record.encryptedAllBallots { true }, errs, stats)
        return errs
    }

    fun verifyEncryptedBallots(ballots: Iterable<EncryptedBallot>, stats : Stats): ErrorMessages {
        val errs = ErrorMessages("verifyDecryptedTally")
        val verifyBallots = VerifyEncryptedBallots(group, manifest, jointPublicKey, He, record.config(), nthreads)
        verifyBallots.verifyBallots(ballots, errs, stats)
        return errs
    }

    fun verifyDecryptedTally(tally: DecryptedTallyOrBallot, stats: Stats): ErrorMessages {
        val errs = ErrorMessages("verifyDecryptedTally")
        val verifyTally = VerifyDecryption(group, manifest, jointPublicKey, He)
        verifyTally.verify(tally, false, errs, stats)
        return errs
    }

    fun verifySpoiledBallotTallies(stats: Stats): ErrorMessages {
        val errs = ErrorMessages("verifyDecryptedTally")
        val verifyTally = VerifyDecryption(group, manifest, jointPublicKey, He)
        verifyTally.verifySpoiledBallotTallies(record.decryptedBallots(), nthreads, errs, stats, true)
        return errs
    }

    fun verifyTallyBallotIds(): Boolean {
        var allOk = true
        val encryptedBallotIds = record.encryptedAllBallots{ it.state == EncryptedBallot.BallotState.CAST }.map { it.ballotId }.toSet()
        val tallyBallotIds = record.encryptedTally()!!.castBallotIds.toSet()
        encryptedBallotIds.forEach {
            if (!tallyBallotIds.contains(it)) {
                println("  tallyBallotIds doesnt contain $it")
                allOk = false
            }
        }
        tallyBallotIds.forEach {
            if (!encryptedBallotIds.contains(it)) {
                println("  encryptedBallotIds doesnt contain $it")
                allOk = false
            }
        }
        return allOk
    }


}
