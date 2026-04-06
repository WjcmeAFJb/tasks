package org.tasks.files

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

class FileHelperExtraTest {

    // ===== uri2String edge cases =====

    @Test
    fun uri2StringNullReturnsEmpty() {
        assertEquals("", FileHelper.uri2String(null))
    }

    @Test
    fun uri2StringFileSchemeWithSpaces() {
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        whenever(uri.path).thenReturn("/path/with spaces/file.txt")
        assertEquals("/path/with spaces/file.txt", FileHelper.uri2String(uri))
    }

    @Test
    fun uri2StringFileSchemeWithSpecialChars() {
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        whenever(uri.path).thenReturn("/path/file-name_v2.txt")
        assertEquals("/path/file-name_v2.txt", FileHelper.uri2String(uri))
    }

    @Test
    fun uri2StringContentSchemeReturnsToString() {
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_CONTENT)
        whenever(uri.toString()).thenReturn("content://com.provider/document/123")
        assertEquals("content://com.provider/document/123", FileHelper.uri2String(uri))
    }

    @Test
    fun uri2StringUnknownSchemeReturnsToString() {
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn("custom")
        whenever(uri.toString()).thenReturn("custom://data")
        assertEquals("custom://data", FileHelper.uri2String(uri))
    }

    // ===== delete edge cases =====

    @Test
    fun deleteNullUriIsNoOp() {
        FileHelper.delete(mock(), null)
    }

    @Test
    fun deleteNullContextAndNullUri() {
        FileHelper.delete(null, null)
    }

    @Test
    fun deleteUnknownSchemeIsNoOp() {
        val context = mock<Context>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn("ftp")
        FileHelper.delete(context, uri) // no-op, no crash
    }

    @Test
    fun deleteFileSchemeDeletesExistingFile() {
        val context = mock<Context>()
        val tempFile = File.createTempFile("delete_test_extra", ".txt")
        assertTrue(tempFile.exists())
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn("file")
        whenever(uri.path).thenReturn(tempFile.absolutePath)
        FileHelper.delete(context, uri)
        assertFalse(tempFile.exists())
    }

    @Test
    fun deleteFileSchemeDeletesChildrenInDirectory() {
        val context = mock<Context>()
        val tempDir = File(System.getProperty("java.io.tmpdir"), "delete_dir_test_${System.nanoTime()}")
        tempDir.mkdirs()
        val child = File(tempDir, "child.txt")
        child.createNewFile()
        assertTrue(tempDir.exists())
        assertTrue(child.exists())
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn("file")
        whenever(uri.path).thenReturn(tempDir.absolutePath)
        FileHelper.delete(context, uri)
        // The private delete method deletes children files but not the directory itself
        assertFalse(child.exists())
        // Clean up
        tempDir.delete()
    }

    @Test
    fun deleteFileSchemeNonexistentFile() {
        val context = mock<Context>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn("file")
        whenever(uri.path).thenReturn("/nonexistent/path/file_${System.nanoTime()}.txt")
        FileHelper.delete(context, uri) // Should not throw
    }

    // ===== getFilename edge cases =====

    @Test
    fun getFilenameFileSchemeReturnsLastSegment() {
        val context = mock<Context>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        whenever(uri.lastPathSegment).thenReturn("report.pdf")
        assertEquals("report.pdf", FileHelper.getFilename(context, uri))
    }

    @Test
    fun getFilenameNullSchemeReturnsNull() {
        val context = mock<Context>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(null)
        assertNull(FileHelper.getFilename(context, uri))
    }

    @Test
    fun getFilenameUnknownSchemeReturnsNull() {
        val context = mock<Context>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn("https")
        assertNull(FileHelper.getFilename(context, uri))
    }

    @Test
    fun getFilenameContentSchemeNullCursorReturnsNull() {
        val context = mock<Context>()
        val contentResolver = mock<ContentResolver>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_CONTENT)
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(contentResolver.query(uri, null, null, null, null)).thenReturn(null)
        assertNull(FileHelper.getFilename(context, uri))
    }

    // ===== fileExists edge cases =====

    @Test
    fun fileExistsFileSchemeNonexistent() = runTest {
        val context = mock<Context>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        whenever(uri.path).thenReturn("/nonexistent_${System.nanoTime()}.txt")
        assertFalse(FileHelper.fileExists(context, uri))
    }

    @Test
    fun fileExistsFileSchemeExisting() = runTest {
        val context = mock<Context>()
        val tempFile = File.createTempFile("exists_extra", ".txt")
        tempFile.writeText("data")
        tempFile.deleteOnExit()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        whenever(uri.path).thenReturn(tempFile.absolutePath)
        assertTrue(FileHelper.fileExists(context, uri))
    }

    @Test
    fun fileExistsNullScheme() = runTest {
        val context = mock<Context>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(null)
        assertFalse(FileHelper.fileExists(context, uri))
    }

    @Test
    fun fileExistsUnknownScheme() = runTest {
        val context = mock<Context>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn("ftp")
        assertFalse(FileHelper.fileExists(context, uri))
    }

    @Test
    fun fileExistsFileSchemeEmptyFile() = runTest {
        val context = mock<Context>()
        val tempFile = File.createTempFile("empty_check", ".txt")
        tempFile.deleteOnExit()
        // empty file still exists
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        whenever(uri.path).thenReturn(tempFile.absolutePath)
        assertTrue(FileHelper.fileExists(context, uri))
    }

    // ===== getMimeType edge cases =====

    @Test
    fun getMimeTypeReturnsContentResolverType() {
        val context = mock<Context>()
        val contentResolver = mock<ContentResolver>()
        val uri = mock<Uri>()
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(contentResolver.getType(uri)).thenReturn("application/json")
        assertEquals("application/json", FileHelper.getMimeType(context, uri))
    }

    @Test
    fun getMimeTypeReturnsPdfType() {
        val context = mock<Context>()
        val contentResolver = mock<ContentResolver>()
        val uri = mock<Uri>()
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(contentResolver.getType(uri)).thenReturn("application/pdf")
        assertEquals("application/pdf", FileHelper.getMimeType(context, uri))
    }

    @Test
    fun getMimeTypeReturnsOctetStream() {
        val context = mock<Context>()
        val contentResolver = mock<ContentResolver>()
        val uri = mock<Uri>()
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(contentResolver.getType(uri)).thenReturn("application/octet-stream")
        assertEquals("application/octet-stream", FileHelper.getMimeType(context, uri))
    }
}
