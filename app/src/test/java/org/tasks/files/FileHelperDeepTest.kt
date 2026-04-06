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
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

/**
 * Deep tests for [FileHelper] methods, focusing on branch coverage.
 */
class FileHelperDeepTest {

    // ===== uri2String =====

    @Test
    fun uri2String_null_returnsEmpty() {
        assertEquals("", FileHelper.uri2String(null))
    }

    @Test
    fun uri2String_fileScheme_returnsAbsolutePath() {
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        `when`(uri.path).thenReturn("/storage/emulated/0/backup.json")
        assertEquals("/storage/emulated/0/backup.json", FileHelper.uri2String(uri))
    }

    @Test
    fun uri2String_contentScheme_returnsUriString() {
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(ContentResolver.SCHEME_CONTENT)
        `when`(uri.toString()).thenReturn("content://authority/path")
        assertEquals("content://authority/path", FileHelper.uri2String(uri))
    }

    @Test
    fun uri2String_httpScheme_returnsUriString() {
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn("http")
        `when`(uri.toString()).thenReturn("http://example.com/file")
        assertEquals("http://example.com/file", FileHelper.uri2String(uri))
    }

    @Test
    fun uri2String_nullScheme_returnsUriString() {
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(null)
        `when`(uri.toString()).thenReturn("some_uri")
        assertEquals("some_uri", FileHelper.uri2String(uri))
    }

    @Test
    fun uri2String_fileScheme_rootPath() {
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        `when`(uri.path).thenReturn("/")
        assertEquals("/", FileHelper.uri2String(uri))
    }

    @Test
    fun uri2String_fileScheme_nestedPath() {
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        `when`(uri.path).thenReturn("/data/data/org.tasks/files/backup.json")
        assertEquals("/data/data/org.tasks/files/backup.json", FileHelper.uri2String(uri))
    }

    @Test
    fun uri2String_androidResourceScheme() {
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn("android.resource")
        `when`(uri.toString()).thenReturn("android.resource://com.example/12345")
        assertEquals("android.resource://com.example/12345", FileHelper.uri2String(uri))
    }

    // ===== delete =====

    @Test
    fun delete_nullUri_doesNotThrow() {
        FileHelper.delete(mock(Context::class.java), null)
    }

    @Test
    fun delete_fileScheme_nonExistentFile() {
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn("file")
        `when`(uri.path).thenReturn("/nonexistent/path/file.txt")
        FileHelper.delete(context, uri)
    }

    @Test
    fun delete_unknownScheme_isNoOp() {
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn("ftp")
        FileHelper.delete(context, uri)
    }

    @Test
    fun delete_nullContextAndNullUri() {
        FileHelper.delete(null, null)
    }

    @Test
    fun delete_fileScheme_existingFile() {
        val context = mock(Context::class.java)
        val tempFile = File.createTempFile("delete_test", ".txt")
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn("file")
        `when`(uri.path).thenReturn(tempFile.absolutePath)
        FileHelper.delete(context, uri)
        assertFalse(tempFile.exists())
    }

    // ===== getFilename =====

    @Test
    fun getFilename_fileScheme_returnsLastPathSegment() {
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        `when`(uri.lastPathSegment).thenReturn("document.pdf")
        assertEquals("document.pdf", FileHelper.getFilename(context, uri))
    }

    @Test
    fun getFilename_fileScheme_nestedPath() {
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        `when`(uri.lastPathSegment).thenReturn("file.pdf")
        assertEquals("file.pdf", FileHelper.getFilename(context, uri))
    }

    @Test
    fun getFilename_contentScheme_nullCursor_returnsNull() {
        val context = mock(Context::class.java)
        val contentResolver = mock(ContentResolver::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(ContentResolver.SCHEME_CONTENT)
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(contentResolver.query(uri, null, null, null, null)).thenReturn(null)
        assertNull(FileHelper.getFilename(context, uri))
    }

    @Test
    fun getFilename_unknownScheme_returnsNull() {
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn("ftp")
        assertNull(FileHelper.getFilename(context, uri))
    }

    @Test
    fun getFilename_nullScheme_returnsNull() {
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(null)
        assertNull(FileHelper.getFilename(context, uri))
    }

    // ===== getMimeType =====

    @Test
    fun getMimeType_returnsContentResolverType() {
        val context = mock(Context::class.java)
        val contentResolver = mock(ContentResolver::class.java)
        val uri = mock(Uri::class.java)
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(contentResolver.getType(uri)).thenReturn("application/json")
        assertEquals("application/json", FileHelper.getMimeType(context, uri))
    }

    @Test
    fun getMimeType_imagePng() {
        val context = mock(Context::class.java)
        val contentResolver = mock(ContentResolver::class.java)
        val uri = mock(Uri::class.java)
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(contentResolver.getType(uri)).thenReturn("image/png")
        assertEquals("image/png", FileHelper.getMimeType(context, uri))
    }

    @Test
    fun getMimeType_textPlain() {
        val context = mock(Context::class.java)
        val contentResolver = mock(ContentResolver::class.java)
        val uri = mock(Uri::class.java)
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(contentResolver.getType(uri)).thenReturn("text/plain")
        assertEquals("text/plain", FileHelper.getMimeType(context, uri))
    }

    @Test
    fun getMimeType_applicationPdf() {
        val context = mock(Context::class.java)
        val contentResolver = mock(ContentResolver::class.java)
        val uri = mock(Uri::class.java)
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(contentResolver.getType(uri)).thenReturn("application/pdf")
        assertEquals("application/pdf", FileHelper.getMimeType(context, uri))
    }

    // ===== fileExists =====

    @Test
    fun fileExists_fileScheme_nonExistent() = runTest {
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        `when`(uri.path).thenReturn("/nonexistent/file.txt")
        assertFalse(FileHelper.fileExists(context, uri))
    }

    @Test
    fun fileExists_fileScheme_existingFile() = runTest {
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)
        val tempFile = File.createTempFile("test_exists", ".txt")
        tempFile.deleteOnExit()
        `when`(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        `when`(uri.path).thenReturn(tempFile.absolutePath)
        assertTrue(FileHelper.fileExists(context, uri))
    }

    @Test
    fun fileExists_unknownScheme() = runTest {
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn("ftp")
        assertFalse(FileHelper.fileExists(context, uri))
    }

    @Test
    fun fileExists_nullScheme() = runTest {
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(null)
        assertFalse(FileHelper.fileExists(context, uri))
    }
}
