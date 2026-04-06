package org.tasks.logging

import co.touchlab.kermit.Severity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

class FileLogWriterTest {

    private lateinit var logDirectory: File
    private lateinit var fileLogWriter: FileLogWriter

    @Before
    fun setUp() {
        logDirectory = kotlin.io.path.createTempDirectory("test-logs").toFile()
        fileLogWriter = FileLogWriter(logDirectory)
    }

    @After
    fun tearDown() {
        logDirectory.deleteRecursively()
    }

    @Test
    fun logDirectoryIsSet() {
        assertEquals(logDirectory, fileLogWriter.logDirectory)
    }

    @Test
    fun logCreatesFiles() {
        // The FileLogWriter uses java.util.logging.FileHandler which creates log files
        fileLogWriter.log(Severity.Info, "test message", "TestTag", null)
        fileLogWriter.flush()

        // FileHandler creates log.0.txt, log.1.txt, etc.
        val logFiles = logDirectory.listFiles { _, name -> name.endsWith(".txt") }
        assertTrue("Expected log files in $logDirectory", logFiles != null && logFiles.isNotEmpty())
    }

    @Test
    fun flushDoesNotThrow() {
        fileLogWriter.flush()
    }

    @Test
    fun logWithThrowableDoesNotThrow() {
        val exception = RuntimeException("test error")
        fileLogWriter.log(Severity.Error, "error occurred", "ErrorTag", exception)
        fileLogWriter.flush()
    }

    @Test
    fun logWithAllSeverities() {
        Severity.entries.forEach { severity ->
            fileLogWriter.log(severity, "msg for $severity", "Tag", null)
        }
        fileLogWriter.flush()
    }

    @Test
    fun zipLogFilesCreatesZipFile() = runTest {
        fileLogWriter.log(Severity.Info, "zip test message", "ZipTag", null)
        fileLogWriter.flush()

        val zipFile = File(logDirectory, "test.zip")
        val result = fileLogWriter.zipLogFiles(zipFile)

        assertTrue(result.exists())
        assertTrue(result.length() > 0)
    }

    @Test
    fun zipLogFilesIncludesLogFiles() = runTest {
        fileLogWriter.log(Severity.Info, "entry in zip", "ZipTag", null)
        fileLogWriter.flush()

        val zipFile = File(logDirectory, "test.zip")
        fileLogWriter.zipLogFiles(zipFile)

        val zip = ZipFile(zipFile)
        val entries = zip.entries().toList()
        assertTrue("Expected at least one entry in zip", entries.isNotEmpty())
        assertTrue(
            "Expected .txt entries",
            entries.any { it.name.endsWith(".txt") }
        )
        zip.close()
    }

    @Test
    fun zipLogFilesWithExtras() = runTest {
        val zipFile = File(logDirectory, "test-extras.zip")
        fileLogWriter.zipLogFiles(zipFile) { zos ->
            zos.putNextEntry(java.util.zip.ZipEntry("extra.txt"))
            zos.write("extra content".toByteArray())
            zos.closeEntry()
        }

        val zip = ZipFile(zipFile)
        val entries = zip.entries().toList().map { it.name }
        assertTrue("Expected extra.txt in zip", entries.contains("extra.txt"))
        zip.close()
    }

    @Test
    fun zipLogFilesReturnsProvidedFile() = runTest {
        val zipFile = File(logDirectory, "return-test.zip")
        val result = fileLogWriter.zipLogFiles(zipFile)

        assertEquals(zipFile, result)
    }

    // --- truncateOrPad companion tests via reflection ---

    @Test
    fun truncateOrPadExactLength() {
        val result = callTruncateOrPad("a".repeat(23))
        assertEquals(23, result.length)
        assertEquals("a".repeat(23), result)
    }

    @Test
    fun truncateOrPadShortString() {
        val result = callTruncateOrPad("short")
        assertEquals(23, result.length)
        assertTrue(result.startsWith("short"))
    }

    @Test
    fun truncateOrPadLongString() {
        val longStr = "a".repeat(50)
        val result = callTruncateOrPad(longStr)
        assertEquals(23, result.length)
        assertTrue(result.contains("..."))
    }

    @Test
    fun truncateOrPadEmptyString() {
        val result = callTruncateOrPad("")
        assertEquals(23, result.length)
    }

    private fun callTruncateOrPad(input: String): String {
        val companionClass = FileLogWriter::class.java.declaredClasses
            .first { it.simpleName == "Companion" }
        val method = companionClass.getDeclaredMethod("truncateOrPad", String::class.java)
        method.isAccessible = true
        val companion = FileLogWriter::class.java.getDeclaredField("Companion")
        companion.isAccessible = true
        return method.invoke(companion.get(null), input) as String
    }
}
