package org.tasks.extensions

import android.database.Cursor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Deep tests for Cursor.kt extension functions covering additional branches.
 */
class CursorExtensionsDeepTest {

    // ===== getString =====

    @Test
    fun getString_columnExists_returnsValue() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("title")).thenReturn(0)
        `when`(cursor.getString(0)).thenReturn("My Task")
        assertEquals("My Task", cursor.getString("title"))
    }

    @Test
    fun getString_columnNotFound_returnsNull() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("nonexistent")).thenReturn(-1)
        assertNull(cursor.getString("nonexistent"))
    }

    @Test
    fun getString_columnAtIndexZero_returnsValue() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("col0")).thenReturn(0)
        `when`(cursor.getString(0)).thenReturn("zero")
        assertEquals("zero", cursor.getString("col0"))
    }

    @Test
    fun getString_returnsNullValue() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("nullable")).thenReturn(3)
        `when`(cursor.getString(3)).thenReturn(null)
        assertNull(cursor.getString("nullable"))
    }

    @Test
    fun getString_returnsEmptyString() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("empty")).thenReturn(1)
        `when`(cursor.getString(1)).thenReturn("")
        assertEquals("", cursor.getString("empty"))
    }

    @Test
    fun getString_unicode() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("name")).thenReturn(2)
        `when`(cursor.getString(2)).thenReturn("\u00e9\u00e8\u00ea")
        assertEquals("\u00e9\u00e8\u00ea", cursor.getString("name"))
    }

    @Test
    fun getString_longString() {
        val cursor = mock(Cursor::class.java)
        val longStr = "a".repeat(10000)
        `when`(cursor.getColumnIndex("long")).thenReturn(0)
        `when`(cursor.getString(0)).thenReturn(longStr)
        assertEquals(longStr, cursor.getString("long"))
    }

    // ===== getLongOrNull =====

    @Test
    fun getLongOrNull_columnExists_returnsValue() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("id")).thenReturn(0)
        `when`(cursor.isNull(0)).thenReturn(false)
        `when`(cursor.getLong(0)).thenReturn(42L)
        assertEquals(42L, cursor.getLongOrNull("id"))
    }

    @Test
    fun getLongOrNull_columnNotFound_returnsNull() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("missing")).thenReturn(-1)
        assertNull(cursor.getLongOrNull("missing"))
    }

    @Test
    fun getLongOrNull_columnIsNull_returnsNull() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("nullable_id")).thenReturn(1)
        `when`(cursor.isNull(1)).thenReturn(true)
        assertNull(cursor.getLongOrNull("nullable_id"))
    }

    @Test
    fun getLongOrNull_zero() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("zero")).thenReturn(0)
        `when`(cursor.isNull(0)).thenReturn(false)
        `when`(cursor.getLong(0)).thenReturn(0L)
        assertEquals(0L, cursor.getLongOrNull("zero"))
    }

    @Test
    fun getLongOrNull_negative() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("neg")).thenReturn(2)
        `when`(cursor.isNull(2)).thenReturn(false)
        `when`(cursor.getLong(2)).thenReturn(-100L)
        assertEquals(-100L, cursor.getLongOrNull("neg"))
    }

    @Test
    fun getLongOrNull_maxValue() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("max")).thenReturn(0)
        `when`(cursor.isNull(0)).thenReturn(false)
        `when`(cursor.getLong(0)).thenReturn(Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, cursor.getLongOrNull("max"))
    }

    @Test
    fun getLongOrNull_minValue() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("min")).thenReturn(0)
        `when`(cursor.isNull(0)).thenReturn(false)
        `when`(cursor.getLong(0)).thenReturn(Long.MIN_VALUE)
        assertEquals(Long.MIN_VALUE, cursor.getLongOrNull("min"))
    }
}
