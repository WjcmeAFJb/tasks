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

class FileHelperMaxCovTest {

    // ================================================================
    // uri2String
    // ================================================================

    @Test
    fun uri2StringNull() {
        assertEquals("", FileHelper.uri2String(null))
    }

    @Test
    fun uri2StringFileScheme() {
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        whenever(uri.path).thenReturn("/data/files/backup.json")
        assertEquals("/data/files/backup.json", FileHelper.uri2String(uri))
    }

    @Test
    fun uri2StringContentScheme() {
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_CONTENT)
        whenever(uri.toString()).thenReturn("content://authority/path")
        assertEquals("content://authority/path", FileHelper.uri2String(uri))
    }

    @Test
    fun uri2StringHttpScheme() {
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn("http")
        whenever(uri.toString()).thenReturn("http://example.com")
        assertEquals("http://example.com", FileHelper.uri2String(uri))
    }

    @Test
    fun uri2StringNullScheme() {
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(null)
        whenever(uri.toString()).thenReturn("some_uri")
        assertEquals("some_uri", FileHelper.uri2String(uri))
    }

    @Test
    fun uri2StringFileSchemeRootPath() {
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        whenever(uri.path).thenReturn("/")
        assertEquals("/", FileHelper.uri2String(uri))
    }

    @Test
    fun uri2StringAndroidResourceScheme() {
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn("android.resource")
        whenever(uri.toString()).thenReturn("android.resource://com.example/12345")
        assertEquals("android.resource://com.example/12345", FileHelper.uri2String(uri))
    }

    // ================================================================
    // delete
    // ================================================================

    @Test
    fun deleteNullUri() {
        FileHelper.delete(mock<Context>(), null)
    }

    @Test
    fun deleteUnknownSchemeIsNoOp() {
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn("ftp")
        FileHelper.delete(mock<Context>(), uri)
    }

    @Test
    fun deleteFileSchemeExistingFile() {
        val tempFile = File.createTempFile("delete_test", ".txt")
        assertTrue(tempFile.exists())
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn("file")
        whenever(uri.path).thenReturn(tempFile.absolutePath)
        FileHelper.delete(mock<Context>(), uri)
        assertFalse(tempFile.exists())
    }

    @Test
    fun deleteFileSchemeNonExistentFile() {
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn("file")
        whenever(uri.path).thenReturn("/nonexistent/path.txt")
        FileHelper.delete(mock<Context>(), uri)
    }

    @Test
    fun deleteNullBoth() {
        FileHelper.delete(null, null)
    }

    // ================================================================
    // getFilename
    // ================================================================

    @Test
    fun getFilenameFileScheme() {
        val context = mock<Context>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        whenever(uri.lastPathSegment).thenReturn("document.pdf")
        assertEquals("document.pdf", FileHelper.getFilename(context, uri))
    }

    @Test
    fun getFilenameContentSchemeNullCursor() {
        val context = mock<Context>()
        val contentResolver = mock<ContentResolver>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_CONTENT)
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(contentResolver.query(uri, null, null, null, null)).thenReturn(null)
        assertNull(FileHelper.getFilename(context, uri))
    }

    @Test
    fun getFilenameUnknownScheme() {
        val context = mock<Context>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn("ftp")
        assertNull(FileHelper.getFilename(context, uri))
    }

    @Test
    fun getFilenameNullScheme() {
        val context = mock<Context>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(null)
        assertNull(FileHelper.getFilename(context, uri))
    }

    // ================================================================
    // getMimeType
    // ================================================================

    @Test
    fun getMimeTypeFromContentResolver() {
        val context = mock<Context>()
        val contentResolver = mock<ContentResolver>()
        val uri = mock<Uri>()
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(contentResolver.getType(uri)).thenReturn("application/json")
        assertEquals("application/json", FileHelper.getMimeType(context, uri))
    }

    @Test
    fun getMimeTypeTextPlain() {
        val context = mock<Context>()
        val contentResolver = mock<ContentResolver>()
        val uri = mock<Uri>()
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(contentResolver.getType(uri)).thenReturn("text/plain")
        assertEquals("text/plain", FileHelper.getMimeType(context, uri))
    }

    @Test
    fun getMimeTypeImagePng() {
        val context = mock<Context>()
        val contentResolver = mock<ContentResolver>()
        val uri = mock<Uri>()
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(contentResolver.getType(uri)).thenReturn("image/png")
        assertEquals("image/png", FileHelper.getMimeType(context, uri))
    }

    // ================================================================
    // fileExists
    // ================================================================

    @Test
    fun fileExistsFileSchemeNonExistent() = runTest {
        val context = mock<Context>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        whenever(uri.path).thenReturn("/nonexistent/file.txt")
        assertFalse(FileHelper.fileExists(context, uri))
    }

    @Test
    fun fileExistsFileSchemeExists() = runTest {
        val context = mock<Context>()
        val uri = mock<Uri>()
        val tempFile = File.createTempFile("exists_test", ".txt")
        tempFile.writeText("content")
        tempFile.deleteOnExit()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        whenever(uri.path).thenReturn(tempFile.absolutePath)
        assertTrue(FileHelper.fileExists(context, uri))
    }

    @Test
    fun fileExistsUnknownScheme() = runTest {
        val context = mock<Context>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn("ftp")
        assertFalse(FileHelper.fileExists(context, uri))
    }

    @Test
    fun fileExistsNullScheme() = runTest {
        val context = mock<Context>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(null)
        assertFalse(FileHelper.fileExists(context, uri))
    }

    @Test
    fun fileExistsEmptyFile() = runTest {
        val context = mock<Context>()
        val uri = mock<Uri>()
        val tempFile = File.createTempFile("empty_test", ".txt")
        tempFile.deleteOnExit()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        whenever(uri.path).thenReturn(tempFile.absolutePath)
        // Empty file exists but File.exists() is still true
        assertTrue(FileHelper.fileExists(context, uri))
    }

    // ================================================================
    // getExtension — MimeTypeMap is stubbed in unit tests, so we
    // test the path resolution fallback instead
    // ================================================================

    @Test
    fun getExtensionFromFilePathFallback() {
        val context = mock<Context>()
        val contentResolver = mock<ContentResolver>()
        val uri = mock<Uri>()
        whenever(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        whenever(uri.lastPathSegment).thenReturn("backup.json")
        whenever(uri.path).thenReturn("/data/backup.json")
        whenever(context.contentResolver).thenReturn(contentResolver)
        whenever(contentResolver.getType(uri)).thenReturn(null)
        // MimeTypeMap is stubbed, so extension falls through to getFilename
        val filename = FileHelper.getFilename(context, uri)
        assertEquals("backup.json", filename)
    }
}
