package org.tasks.calendars

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.preferences.PermissionChecker

class CalendarProviderTest {

    private lateinit var context: Context
    private lateinit var permissionChecker: PermissionChecker
    private lateinit var contentResolver: ContentResolver
    private lateinit var calendarProvider: CalendarProvider

    @Before
    fun setUp() {
        context = mock()
        permissionChecker = mock()
        contentResolver = mock()
        `when`(context.contentResolver).thenReturn(contentResolver)

        calendarProvider = CalendarProvider(context, permissionChecker)
    }

    // --- getCalendars ---

    @Test
    fun getCalendarsReturnsEmptyWhenNoPermission() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(false)

        val result = calendarProvider.calendars

        assertTrue(result.isEmpty())
    }

    @Test
    fun getCalendarsReturnsEmptyWhenCursorIsNull() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(true)
        `when`(contentResolver.query(any(), any(), anyString(), isNull(), anyString()))
            .thenReturn(null)

        val result = calendarProvider.calendars

        assertTrue(result.isEmpty())
    }

    @Test
    fun getCalendarsReturnsEmptyWhenCursorIsEmpty() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(true)
        val cursor = mock(Cursor::class.java)
        `when`(cursor.count).thenReturn(0)
        `when`(contentResolver.query(any(), any(), anyString(), isNull(), anyString()))
            .thenReturn(cursor)

        val result = calendarProvider.calendars

        assertTrue(result.isEmpty())
    }

    @Test
    fun getCalendarsReturnsParsedCalendars() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(true)
        val cursor = mock(Cursor::class.java)
        `when`(cursor.count).thenReturn(1)
        `when`(cursor.moveToNext()).thenReturn(true, false)
        `when`(cursor.getColumnIndex("_id")).thenReturn(0)
        `when`(cursor.getColumnIndex("calendar_displayName")).thenReturn(1)
        `when`(cursor.getColumnIndex("calendar_color")).thenReturn(2)
        `when`(cursor.getString(0)).thenReturn("5")
        `when`(cursor.getString(1)).thenReturn("Work")
        `when`(cursor.getInt(2)).thenReturn(0xFF0000)
        `when`(contentResolver.query(any(), any(), anyString(), isNull(), anyString()))
            .thenReturn(cursor)

        val result = calendarProvider.calendars

        assertEquals(1, result.size)
        assertEquals("5", result[0].id)
        assertEquals("Work", result[0].name)
        assertEquals(0xFF0000, result[0].color)
    }

    @Test
    fun getCalendarsHandlesException() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(true)
        `when`(contentResolver.query(any(), any(), anyString(), isNull(), anyString()))
            .thenThrow(RuntimeException("test exception"))

        val result = calendarProvider.calendars

        assertTrue(result.isEmpty())
    }

    // --- getCalendar ---

    @Test
    fun getCalendarReturnsNullForNullId() {
        val result = calendarProvider.getCalendar(null)

        assertNull(result)
    }

    @Test
    fun getCalendarReturnsNullForEmptyId() {
        val result = calendarProvider.getCalendar("")

        assertNull(result)
    }

    @Test
    fun getCalendarReturnsNullWhenNoPermission() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(false)

        val result = calendarProvider.getCalendar("1")

        assertNull(result)
    }

    @Test
    fun getCalendarReturnsNullWhenNotFound() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(true)
        `when`(contentResolver.query(any(), any(), anyString(), isNull(), anyString()))
            .thenReturn(null)

        val result = calendarProvider.getCalendar("999")

        assertNull(result)
    }

    @Test
    fun getCalendarReturnsCalendar() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(true)
        val cursor = mock(Cursor::class.java)
        `when`(cursor.count).thenReturn(1)
        `when`(cursor.moveToNext()).thenReturn(true, false)
        `when`(cursor.getColumnIndex("_id")).thenReturn(0)
        `when`(cursor.getColumnIndex("calendar_displayName")).thenReturn(1)
        `when`(cursor.getColumnIndex("calendar_color")).thenReturn(2)
        `when`(cursor.getString(0)).thenReturn("3")
        `when`(cursor.getString(1)).thenReturn("Personal")
        `when`(cursor.getInt(2)).thenReturn(0x00FF00)
        `when`(contentResolver.query(any(), any(), anyString(), isNull(), anyString()))
            .thenReturn(cursor)

        val result = calendarProvider.getCalendar("3")

        assertEquals("3", result?.id)
        assertEquals("Personal", result?.name)
    }
}
