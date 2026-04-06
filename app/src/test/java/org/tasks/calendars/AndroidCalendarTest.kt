package org.tasks.calendars

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidCalendarTest {

    @Test
    fun gettersReturnConstructorValues() {
        val calendar = AndroidCalendar("1", "My Calendar", 0xFF0000)
        assertEquals("1", calendar.id)
        assertEquals("My Calendar", calendar.name)
        assertEquals(0xFF0000, calendar.color)
    }

    @Test
    fun nullId() {
        val calendar = AndroidCalendar(null, "Cal", 0)
        assertNull(calendar.id)
    }

    @Test
    fun nullName() {
        val calendar = AndroidCalendar("1", null, 0)
        assertNull(calendar.name)
    }

    @Test
    fun zeroColor() {
        val calendar = AndroidCalendar("1", "Cal", 0)
        assertEquals(0, calendar.color)
    }

    @Test
    fun negativeColor() {
        val calendar = AndroidCalendar("1", "Cal", -16777216)
        assertEquals(-16777216, calendar.color)
    }

    @Test
    fun toStringContainsId() {
        val calendar = AndroidCalendar("42", "Test", 0)
        assertTrue(calendar.toString().contains("42"))
    }

    @Test
    fun toStringContainsName() {
        val calendar = AndroidCalendar("1", "Work Calendar", 0)
        assertTrue(calendar.toString().contains("Work Calendar"))
    }

    @Test
    fun toStringContainsColor() {
        val calendar = AndroidCalendar("1", "Cal", 12345)
        assertTrue(calendar.toString().contains("12345"))
    }

    @Test
    fun toStringFormat() {
        val calendar = AndroidCalendar("1", "Cal", 99)
        val expected = "AndroidCalendar{id='1', name='Cal', color=99}"
        assertEquals(expected, calendar.toString())
    }

    @Test
    fun twoCalendarsWithSameValuesAreNotEqual() {
        val c1 = AndroidCalendar("1", "Cal", 0)
        val c2 = AndroidCalendar("1", "Cal", 0)
        assertNotEquals(c1, c2)
    }

    @Test
    fun emptyStrings() {
        val calendar = AndroidCalendar("", "", 0)
        assertEquals("", calendar.id)
        assertEquals("", calendar.name)
    }

    @Test
    fun maxIntColor() {
        val calendar = AndroidCalendar("1", "Cal", Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE, calendar.color)
    }

    @Test
    fun minIntColor() {
        val calendar = AndroidCalendar("1", "Cal", Int.MIN_VALUE)
        assertEquals(Int.MIN_VALUE, calendar.color)
    }
}
