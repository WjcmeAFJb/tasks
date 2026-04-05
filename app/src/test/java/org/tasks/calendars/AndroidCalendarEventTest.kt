package org.tasks.calendars

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidCalendarEventTest {

    @Test
    fun gettersReturnConstructorValues() {
        val event = AndroidCalendarEvent(42L, "Meeting", 1000L, 2000L, 5)
        assertEquals(42L, event.id)
        assertEquals("Meeting", event.title)
        assertEquals(1000L, event.start)
        assertEquals(2000L, event.end)
        assertEquals(5, event.calendarId)
    }

    @Test
    fun nullTitle() {
        val event = AndroidCalendarEvent(1L, null, 0L, 0L, 0)
        assertNull(event.title)
    }

    @Test
    fun toStringContainsId() {
        val event = AndroidCalendarEvent(99L, "Test", 100L, 200L, 1)
        assertTrue(event.toString().contains("99"))
    }

    @Test
    fun toStringContainsTitle() {
        val event = AndroidCalendarEvent(1L, "My Event", 100L, 200L, 1)
        assertTrue(event.toString().contains("My Event"))
    }

    @Test
    fun toStringContainsStartAndEnd() {
        val event = AndroidCalendarEvent(1L, "Test", 12345L, 67890L, 1)
        val str = event.toString()
        assertTrue(str.contains("12345"))
        assertTrue(str.contains("67890"))
    }

    @Test
    fun toStringContainsCalendarId() {
        val event = AndroidCalendarEvent(1L, "Test", 0L, 0L, 77)
        assertTrue(event.toString().contains("77"))
    }

    @Test
    fun toStringFormat() {
        val event = AndroidCalendarEvent(1L, "Title", 100L, 200L, 3)
        val expected = "AndroidCalendarEvent{id=1, title='Title', start=100, end=200, calendarId=3}"
        assertEquals(expected, event.toString())
    }

    @Test
    fun zeroValues() {
        val event = AndroidCalendarEvent(0L, "", 0L, 0L, 0)
        assertEquals(0L, event.id)
        assertEquals("", event.title)
        assertEquals(0L, event.start)
        assertEquals(0L, event.end)
        assertEquals(0, event.calendarId)
    }

    @Test
    fun negativeId() {
        val event = AndroidCalendarEvent(-1L, "Test", -100L, -50L, -1)
        assertEquals(-1L, event.id)
        assertEquals(-100L, event.start)
        assertEquals(-50L, event.end)
        assertEquals(-1, event.calendarId)
    }

    @Test
    fun maxLongValues() {
        val event = AndroidCalendarEvent(
            Long.MAX_VALUE, "Test", Long.MAX_VALUE, Long.MAX_VALUE, Int.MAX_VALUE,
        )
        assertEquals(Long.MAX_VALUE, event.id)
        assertEquals(Long.MAX_VALUE, event.start)
        assertEquals(Long.MAX_VALUE, event.end)
        assertEquals(Int.MAX_VALUE, event.calendarId)
    }

    @Test
    fun twoEventsWithSameValuesAreNotEqual() {
        val event1 = AndroidCalendarEvent(1L, "Test", 100L, 200L, 1)
        val event2 = AndroidCalendarEvent(1L, "Test", 100L, 200L, 1)
        assertNotEquals(event1, event2)
    }
}
