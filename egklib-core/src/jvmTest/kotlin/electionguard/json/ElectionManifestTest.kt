package electionguard.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.testResourcesDir
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.Test
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.spi.FileSystemProvider
import kotlin.test.assertTrue

@OptIn(ExperimentalSerializationApi::class)
class ElectionManifestTest {

    var fileSystem: FileSystem = FileSystems.getDefault()
    var fileSystemProvider: FileSystemProvider = fileSystem.provider()
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true }

    @Test
    fun test() {
        val manifest = readElectionManifest("$testResourcesDir/json/election_manifest_pretty.json")
        assertTrue(manifest is Ok)
        println("ElectionManifest = ${manifest.unwrap()}")
    }

    private fun readElectionManifest(filename: String): Result<ElectionManifestJsonR, String> =
        try {
            var manifest: ElectionManifestJsonR
            val path = Path.of(filename)
            fileSystemProvider.newInputStream(path).use { inp ->
                manifest = jsonReader.decodeFromStream<ElectionManifestJsonR>(inp)
            }
            Ok(manifest)
        } catch (e: Exception) {
            e.printStackTrace()
            Err(e.message ?: "readElectionManifest $filename error")
        }
}
