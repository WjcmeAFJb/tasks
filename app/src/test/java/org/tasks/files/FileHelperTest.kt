package org.tasks.files

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Tests for [FileHelper].
 *
 * Many methods in FileHelper depend on Android framework classes (Uri.parse,
 * Intent, DocumentFile, ContentResolver) which are stubbed in unit tests.
 * We test the methods that can work with mocked objects.
 */
class FileHelperTest {

    // ===== uri2String tests =====

    @Test
    fun uri2StringReturnsEmptyForNull() {
        assertEquals("", FileHelper.uri2String(null))
    }

    @Test
    fun uri2StringWithContentSchemeReturnsToString() {
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(ContentResolver.SCHEME_CONTENT)
        `when`(uri.toString()).thenReturn("content://com.example/test")
        val result = FileHelper.uri2String(uri)
        assertEquals("content://com.example/test", result)
    }

    @Test
    fun uri2StringWithFileSchemeReturnsAbsolutePath() {
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        `when`(uri.path).thenReturn("/tmp/test.txt")
        val result = FileHelper.uri2String(uri)
        assertEquals("/tmp/test.txt", result)
    }

    @Test
    fun uri2StringWithHttpSchemeReturnsToString() {
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn("http")
        `when`(uri.toString()).thenReturn("http://example.com/file.txt")
        val result = FileHelper.uri2String(uri)
        assertEquals("http://example.com/file.txt", result)
    }

    @Test
    fun uri2StringWithAndroidResourceSchemeReturnsToString() {
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn("android.resource")
        `when`(uri.toString()).thenReturn("android.resource://com.example/12345")
        val result = FileHelper.uri2String(uri)
        assertEquals("android.resource://com.example/12345", result)
    }

    @Test
    fun uri2StringWithFileSchemeNestedPath() {
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        `when`(uri.path).thenReturn("/data/data/org.tasks/files/backup.json")
        val result = FileHelper.uri2String(uri)
        assertEquals("/data/data/org.tasks/files/backup.json", result)
    }

    // ===== delete with null tests =====

    @Test
    fun deleteWithNullUriDoesNothing() {
        // Should not throw
        FileHelper.delete(null, null)
    }

    @Test
    fun deleteWithNullContextAndNullUriDoesNothing() {
        FileHelper.delete(null, null)
    }

    // ===== getFilename tests =====

    @Test
    fun getFilenameWithFileScheme() {
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        `when`(uri.lastPathSegment).thenReturn("test.txt")
        val result = FileHelper.getFilename(context, uri)
        assertEquals("test.txt", result)
    }

    @Test
    fun getFilenameWithFileSchemeNestedPath() {
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(ContentResolver.SCHEME_FILE)
        `when`(uri.lastPathSegment).thenReturn("file.pdf")
        val result = FileHelper.getFilename(context, uri)
        assertEquals("file.pdf", result)
    }

    @Test
    fun getFilenameWithUnknownSchemeReturnsNull() {
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn("http")
        val result = FileHelper.getFilename(context, uri)
        assertNull(result)
    }

    @Test
    fun getFilenameWithNullSchemeReturnsNull() {
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(null)
        val result = FileHelper.getFilename(context, uri)
        assertNull(result)
    }

    @Test
    fun getFilenameWithContentSchemeNullCursorReturnsNull() {
        val context = mock(Context::class.java)
        val contentResolver = mock(ContentResolver::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn(ContentResolver.SCHEME_CONTENT)
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(contentResolver.query(uri, null, null, null, null)).thenReturn(null)
        val result = FileHelper.getFilename(context, uri)
        assertNull(result)
    }

    // ===== getMimeType tests =====

    @Test
    fun getMimeTypeReturnsContentResolverType() {
        val context = mock(Context::class.java)
        val contentResolver = mock(ContentResolver::class.java)
        val uri = mock(Uri::class.java)
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(contentResolver.getType(uri)).thenReturn("application/pdf")
        val result = FileHelper.getMimeType(context, uri)
        assertEquals("application/pdf", result)
    }

    @Test
    fun getMimeTypeReturnsNonNullTypeFromContentResolver() {
        val context = mock(Context::class.java)
        val contentResolver = mock(ContentResolver::class.java)
        val uri = mock(Uri::class.java)
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(contentResolver.getType(uri)).thenReturn("image/png")
        val result = FileHelper.getMimeType(context, uri)
        assertEquals("image/png", result)
    }

    @Test
    fun getMimeTypeReturnsTextPlain() {
        val context = mock(Context::class.java)
        val contentResolver = mock(ContentResolver::class.java)
        val uri = mock(Uri::class.java)
        `when`(context.contentResolver).thenReturn(contentResolver)
        `when`(contentResolver.getType(uri)).thenReturn("text/plain")
        val result = FileHelper.getMimeType(context, uri)
        assertEquals("text/plain", result)
    }

    // ===== delete with file scheme (real File objects) =====

    @Test
    fun deleteWithFileSchemeUri() {
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn("file")
        `when`(uri.path).thenReturn("/tmp/nonexistent_test_file_12345.txt")
        // Should not throw even if file doesn't exist
        FileHelper.delete(context, uri)
    }

    // ===== delete with unknown scheme (no-op) =====

    @Test
    fun deleteWithUnknownSchemeIsNoOp() {
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)
        `when`(uri.scheme).thenReturn("http")
        // Should not throw for unknown scheme
        FileHelper.delete(context, uri)
    }
}
