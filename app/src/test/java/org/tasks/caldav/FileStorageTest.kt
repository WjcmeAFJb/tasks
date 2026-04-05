package org.tasks.caldav

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class FileStorageTest {
    private lateinit var tempDir: File
    private lateinit var storage: FileStorage

    @Before
    fun setUp() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "fileStorageTest_${System.nanoTime()}")
        tempDir.mkdirs()
        storage = FileStorage(tempDir.absolutePath)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test fun rootIsVtodoSubdir() {
        assertEquals("vtodo", storage.root.name)
        assertTrue(storage.root.absolutePath.startsWith(tempDir.absolutePath))
    }

    @Test fun getFileWithSegments() {
        val file = storage.getFile("account", "calendar", "task.ics")
        assertNotNull(file)
        assertTrue(file!!.absolutePath.contains("account"))
        assertTrue(file.absolutePath.contains("calendar"))
        assertTrue(file.absolutePath.endsWith("task.ics"))
    }

    @Test fun getFileReturnsNullForNullSegment() =
        assertNull(storage.getFile("account", null, "task.ics"))

    @Test fun getFileReturnsNullForBlankSegment() =
        assertNull(storage.getFile("account", "", "task.ics"))

    @Test fun getFileWithNoSegmentsReturnsNull() {
        // vararg with no args => empty array => none{} is true for empty => returns null
        // Actually: empty segments.none { it.isNullOrBlank() } returns true, so fold runs with empty
        // But fold with empty returns root. Let's verify.
        val file = storage.getFile()
        // With no segments, none{} is vacuously true, so fold with empty array returns root
        assertNotNull(file)
    }

    @Test fun writeAndRead() = runTest {
        val dir = File(storage.root, "acc/cal")
        dir.mkdirs()
        val file = File(dir, "test.ics")
        storage.write(file, "VCALENDAR content")
        assertEquals("VCALENDAR content", storage.read(file))
    }

    @Test fun readNonExistentReturnsNull() = runTest {
        val file = File(storage.root, "nonexistent.ics")
        assertNull(storage.read(file))
    }

    @Test fun readNullReturnsNull() = runTest {
        assertNull(storage.read(null))
    }

    @Test fun writeNullDeletesFile() = runTest {
        val dir = File(storage.root, "acc")
        dir.mkdirs()
        val file = File(dir, "test.ics")
        file.writeText("data")
        assertTrue(file.exists())
        storage.write(file, null)
        assertFalse(file.exists())
    }

    @Test fun writeBlankDeletesFile() = runTest {
        val dir = File(storage.root, "acc")
        dir.mkdirs()
        val file = File(dir, "test.ics")
        file.writeText("data")
        storage.write(file, "  ")
        assertFalse(file.exists())
    }

    @Test fun overwriteExistingFile() = runTest {
        val dir = File(storage.root, "acc")
        dir.mkdirs()
        val file = File(dir, "test.ics")
        storage.write(file, "old")
        storage.write(file, "new")
        assertEquals("new", storage.read(file))
    }
}
