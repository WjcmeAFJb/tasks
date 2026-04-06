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
import org.tasks.data.entity.Task
import org.tasks.preferences.PermissionChecker

class CalendarEventProviderTest {

    private lateinit var context: Context
    private lateinit var permissionChecker: PermissionChecker
    private lateinit var contentResolver: ContentResolver
    private lateinit var calendarEventProvider: CalendarEventProvider

    @Before
    fun setUp() {
        context = mock()
        permissionChecker = mock()
        contentResolver = mock()
        `when`(context.contentResolver).thenReturn(contentResolver)

        calendarEventProvider = CalendarEventProvider(context, permissionChecker)
    }

    // --- getEvent ---

    @Test
    fun getEventReturnsNullWhenNoPermission() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(false)
        val uri = mock(Uri::class.java)

        val result = calendarEventProvider.getEvent(uri)

        assertNull(result)
    }

    @Test
    fun getEventReturnsNullWhenCursorIsNull() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(true)
        val uri = mock(Uri::class.java)
        `when`(contentResolver.query(any(), any(), isNull(), isNull(), isNull()))
            .thenReturn(null)

        val result = calendarEventProvider.getEvent(uri)

        assertNull(result)
    }

    @Test
    fun getEventReturnsNullWhenCursorIsEmpty() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(true)
        val uri = mock(Uri::class.java)
        val cursor = mock(Cursor::class.java)
        `when`(cursor.count).thenReturn(0)
        `when`(contentResolver.query(any(), any(), isNull(), isNull(), isNull()))
            .thenReturn(cursor)

        val result = calendarEventProvider.getEvent(uri)

        assertNull(result)
    }

    @Test
    fun getEventReturnsEvent() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(true)
        val uri = mock(Uri::class.java)
        val cursor = mock(Cursor::class.java)
        `when`(cursor.count).thenReturn(1)
        `when`(cursor.moveToNext()).thenReturn(true, false)
        `when`(cursor.getColumnIndex("_id")).thenReturn(0)
        `when`(cursor.getColumnIndexOrThrow("dtstart")).thenReturn(1)
        `when`(cursor.getColumnIndexOrThrow("dtend")).thenReturn(2)
        `when`(cursor.getColumnIndexOrThrow("title")).thenReturn(3)
        `when`(cursor.getColumnIndexOrThrow("calendar_id")).thenReturn(4)
        `when`(cursor.getLong(0)).thenReturn(10L)
        `when`(cursor.getLong(1)).thenReturn(1000L)
        `when`(cursor.getLong(2)).thenReturn(2000L)
        `when`(cursor.getString(3)).thenReturn("Meeting")
        `when`(cursor.getInt(4)).thenReturn(5)
        `when`(contentResolver.query(any(), any(), isNull(), isNull(), isNull()))
            .thenReturn(cursor)

        val result = calendarEventProvider.getEvent(uri)

        assertEquals(10L, result?.id)
        assertEquals("Meeting", result?.title)
        assertEquals(1000L, result?.start)
        assertEquals(2000L, result?.end)
        assertEquals(5, result?.calendarId)
    }

    // --- getEventsBetween ---

    @Test
    fun getEventsBetweenReturnsEmptyWhenNoPermission() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(false)

        val result = calendarEventProvider.getEventsBetween(1000L, 2000L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun getEventsBetweenReturnsEmptyWhenNoEvents() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(true)
        `when`(contentResolver.query(any(), any(), anyString(), any(), isNull()))
            .thenReturn(null)

        val result = calendarEventProvider.getEventsBetween(1000L, 2000L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun getEventsBetweenHandlesException() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(true)
        `when`(contentResolver.query(any(), any(), anyString(), any(), isNull()))
            .thenThrow(RuntimeException("test error"))

        val result = calendarEventProvider.getEventsBetween(1000L, 2000L)

        assertTrue(result.isEmpty())
    }

    // --- deleteEvent(Task) ---

    @Test
    fun deleteEventNoPermissionReturnsEarly() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(false)
        val task = Task()
        task.calendarURI = "content://calendar/1"

        calendarEventProvider.deleteEvent(task)

        verify(contentResolver, never()).delete(any(), isNull(), isNull())
        // When permission is denied, deleteEvent returns early and does NOT clear the URI
        assertEquals("content://calendar/1", task.calendarURI)
    }

    @Test
    fun deleteEventWithNullUri() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(true)
        val task = Task()
        task.calendarURI = null

        calendarEventProvider.deleteEvent(task)

        verify(contentResolver, never()).delete(any(), isNull(), isNull())
    }

    @Test
    fun deleteEventWithEmptyUri() {
        `when`(permissionChecker.canAccessCalendars()).thenReturn(true)
        val task = Task()
        task.calendarURI = ""

        calendarEventProvider.deleteEvent(task)

        verify(contentResolver, never()).delete(any(), isNull(), isNull())
    }

    // --- deleteEvents(List) ---

    @Test
    fun deleteEventsWithEmptyList() {
        calendarEventProvider.deleteEvents(emptyList())

        verify(contentResolver, never()).delete(any(), isNull(), isNull())
    }

    // --- CalendarPicker companion ---

    @Test
    fun calendarPickerExtraCalendarId() {
        assertEquals("extra_calendar_id", CalendarPicker.EXTRA_CALENDAR_ID)
    }

    @Test
    fun calendarPickerRequestKey() {
        assertEquals("calendar_picker_result", CalendarPicker.REQUEST_KEY)
    }
}
