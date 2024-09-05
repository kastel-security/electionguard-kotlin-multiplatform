package electionguard.core

import node.fs.writeFileSync
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UtilsKommTestJs {

    @Test
    fun testSimpleFileOperations() {

        // this should succeed whether the directories exist or not
        assertTrue { createDirectories("test/directory") }

        assertTrue { pathExists("test/directory") }
        assertTrue { isDirectory("test/directory") }

        // if the file exist, just overwrite it
        writeFileSync("test/directory/test.txt", "Hello, World!")
        assertTrue { pathExists("test/directory/test.txt") }

        fileReadText("test/directory/test.txt").let {
            assertTrue { it == "Hello, World!" }
        }
        assertFalse { isDirectory("test/directory/test.txt") }

        val data = "Hello, World!\nGoodbye, World!\r\nMore text."
        writeFileSync("test/directory/test.txt", data)
        fileReadText("test/directory/test.txt").let {
            assertTrue { it == data }
        }
        fileReadLines("test/directory/test.txt").let {
            assertTrue { it.size == 3 }
            assertTrue { it[0] == "Hello, World!" }
            assertTrue { it[1] == "Goodbye, World!" }
            assertTrue { it[2] == "More text." }
        }
        fileReadBytes("test/directory/test.txt").let {
            assertTrue { it.contentEquals(data.encodeToByteArray()) }
        }
    }

    @Test
    fun accessResources() {
        // the processed resources are put in kotlin subdirectory of the working directory
        // build/js/packages/electionguard-kotlin-multiplatform-egklib-test
        assertTrue { pathExists("kotlin/testFile.txt") }
    }

}
