package org.tasks.extensions

import android.database.Cursor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Tests for Cursor.kt extension functions.
 */
class CursorExtensionsTest {

    // ===== getString by column name =====

    @Test
    fun getStringReturnsValueWhenColumnExists() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("name")).thenReturn(2)
        `when`(cursor.getString(2)).thenReturn("hello")

        assertEquals("hello", cursor.getString("name"))
    }

    @Test
    fun getStringReturnsNullWhenColumnNotFound() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("missing")).thenReturn(-1)

        assertNull(cursor.getString("missing"))
    }

    @Test
    fun getStringReturnsNullWhenValueIsNull() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("name")).thenReturn(0)
        `when`(cursor.getString(0)).thenReturn(null)

        assertNull(cursor.getString("name"))
    }

    @Test
    fun getStringReturnsEmptyStringWhenValueIsEmpty() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("name")).thenReturn(1)
        `when`(cursor.getString(1)).thenReturn("")

        assertEquals("", cursor.getString("name"))
    }

    @Test
    fun getStringAtIndexZero() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("col")).thenReturn(0)
        `when`(cursor.getString(0)).thenReturn("value")

        assertEquals("value", cursor.getString("col"))
    }

    @Test
    fun getStringAtHighIndex() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("col")).thenReturn(100)
        `when`(cursor.getString(100)).thenReturn("high")

        assertEquals("high", cursor.getString("col"))
    }

    // ===== getLongOrNull by column name =====

    @Test
    fun getLongOrNullReturnsValueWhenColumnExists() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("id")).thenReturn(0)
        `when`(cursor.isNull(0)).thenReturn(false)
        `when`(cursor.getLong(0)).thenReturn(42L)

        assertEquals(42L, cursor.getLongOrNull("id"))
    }

    @Test
    fun getLongOrNullReturnsNullWhenColumnNotFound() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("missing")).thenReturn(-1)

        assertNull(cursor.getLongOrNull("missing"))
    }

    @Test
    fun getLongOrNullReturnsNullWhenValueIsNull() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("id")).thenReturn(3)
        `when`(cursor.isNull(3)).thenReturn(true)

        assertNull(cursor.getLongOrNull("id"))
    }

    @Test
    fun getLongOrNullReturnsZero() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("id")).thenReturn(1)
        `when`(cursor.isNull(1)).thenReturn(false)
        `when`(cursor.getLong(1)).thenReturn(0L)

        assertEquals(0L, cursor.getLongOrNull("id"))
    }

    @Test
    fun getLongOrNullReturnsNegativeValue() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("id")).thenReturn(2)
        `when`(cursor.isNull(2)).thenReturn(false)
        `when`(cursor.getLong(2)).thenReturn(-1L)

        assertEquals(-1L, cursor.getLongOrNull("id"))
    }

    @Test
    fun getLongOrNullReturnsLargeValue() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("timestamp")).thenReturn(0)
        `when`(cursor.isNull(0)).thenReturn(false)
        `when`(cursor.getLong(0)).thenReturn(Long.MAX_VALUE)

        assertEquals(Long.MAX_VALUE, cursor.getLongOrNull("timestamp"))
    }

    @Test
    fun getLongOrNullAtIndexZero() {
        val cursor = mock(Cursor::class.java)
        `when`(cursor.getColumnIndex("col")).thenReturn(0)
        `when`(cursor.isNull(0)).thenReturn(false)
        `when`(cursor.getLong(0)).thenReturn(123L)

        assertEquals(123L, cursor.getLongOrNull("col"))
    }
}
