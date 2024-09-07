package electionguard.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.testResourcesDir
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.Test
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.spi.FileSystemProvider
import kotlin.test.assertTrue

class ElectionParametersTest {

    var fileSystem: FileSystem = FileSystems.getDefault()
    var fileSystemProvider: FileSystemProvider = fileSystem.provider()
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true }

    @Test
    fun test() {
        val params = readElectionParameters("$testResourcesDir/json/election_parameters.json")
        println("ElectionParameters = $params")
        assertTrue { params is Ok }
    }

    private fun readElectionParameters(filename: String): Result<ElectionParameters, String> =
        try {
            var electionParams: ElectionParameters
            val path = Path.of(filename)
            fileSystemProvider.newInputStream(path).use { inp ->
                val json = jsonReader.decodeFromStream<ElectionParametersJsonR>(inp)
                electionParams = json.import()
            }
            Ok(electionParams)
        } catch (e: Exception) {
            e.printStackTrace()
            Err(e.message ?: "readElectionParameters $filename error")
        }
}
